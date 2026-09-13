package com.huanglongmao.onlinefmradio.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.DefaultMediaNotificationProvider
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.huanglongmao.onlinefmradio.MainActivity
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.data.model.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * 后台音频服务：MediaSessionService + ExoPlayer
 * （对应 Flutter 版 RadioAudioHandler + audio_player_singleton + audio_session_setup）。
 *
 * 完整支持：
 * - 后台播放（前台服务，foregroundServiceType=mediaPlayback）
 * - 锁屏 / 通知栏 / 线控耳机 / 蓝牙媒体控制
 * - 音频焦点与来电打断（Media3 自动处理）
 * - 直播流断流自动重连（指数退避，最多 4 次）
 * - 重连耗尽后经 PlayerEventBus 上抛友好错误
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var reconnect: ReconnectManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 最近一次底层错误，用于重连耗尽时生成友好文案 */
    @Volatile
    private var lastError: PlaybackException? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // 缓冲参数对齐原版 AndroidLoadControl：
        // min 50s / max 90s / 起播 1500ms / 再缓冲 3s / back 10s
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50_000,   // minBufferMs
                90_000,   // maxBufferMs
                1_500,    // bufferForPlaybackMs
                3_000,    // bufferForPlaybackAfterRebufferMs
            )
            .setBackBuffer(10_000, true)
            .build()

        val player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true) // 拔耳机自动暂停
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        val reconnectManager = ReconnectManager(
            play = { station -> playStationInternal(station) },
            scope = serviceScope,
        )
        reconnect = reconnectManager

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // 直播流"结束"意味着服务端断开连接 → 自动重连而非停止
                if (playbackState == Player.STATE_ENDED) {
                    reconnectManager.onStreamDropped(null)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // 切台（UI 播放 / 队列跳转）时更新重连目标，并清零重连状态
                val station = MediaItemMapper.toStation(mediaItem)
                if (station != null) {
                    reconnectManager.setStation(station)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // 成功恢复播放：清零重连计数与"重连中"状态
                if (isPlaying) {
                    reconnectManager.onPlaybackRestored()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                lastError = error
                reconnectManager.onStreamDropped(error)
            }
        })

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(getActivityPendingIntent())
            .setCallback(SessionCallback())
            .build()

        val notificationProvider =
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(AppConstants.AUDIO_NOTIFICATION_CHANNEL_ID)
                .build()
        notificationProvider.setSmallIcon(com.huanglongmao.onlinefmradio.R.drawable.ic_stat_radio)
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /** 统一播放入口：加载流并起播（含重连复用） */
    private fun playStationInternal(station: Station) {
        val player = mediaSession?.player ?: return
        player.setMediaItem(MediaItemMapper.toMediaItem(station), 0)
        player.prepare()
        player.play()
    }

    /**
     * 直播流断开（STATE_ENDED 或 error）时触发自动重连。
     * 指数退避：1s → 2s → 4s → 8s；超过 4 次上抛友好错误。
     */
    private inner class ReconnectManager(
        private val play: (Station) -> Unit,
        private val scope: CoroutineScope,
    ) {
        @Volatile
        private var station: Station? = null

        @Volatile
        private var attempts = 0
        private var job: Job? = null

        fun setStation(newStation: Station) {
            station = newStation
            attempts = 0
            cancelPending()
            PlayerEventBus.reconnecting.value = false
        }

        fun onStreamDropped(error: PlaybackException?) {
            val target = station ?: return
            if (attempts >= MAX_RECONNECT) {
                PlayerEventBus.reconnecting.value = false
                PlayerEventBus.tryEmitError(friendlyError(error ?: lastError))
                return
            }
            attempts++
            PlayerEventBus.reconnecting.value = true
            cancelPending()
            job = scope.launch {
                delay(1000L shl (attempts - 1)) // 1, 2, 4, 8 秒
                play(target)
            }
        }

        fun onPlaybackRestored() {
            attempts = 0
            PlayerEventBus.reconnecting.value = false
        }

        fun cancelPending() {
            job?.cancel()
            job = null
        }

        fun clear() {
            cancelPending()
            station = null
            attempts = 0
            PlayerEventBus.reconnecting.value = false
        }
    }

    /** 将底层播放错误映射为用户可读文案（对应原版 _friendlyError） */
    private fun friendlyError(error: PlaybackException?): String {
        val cause = error?.cause
        return when {
            cause is HttpDataSource.InvalidResponseCodeException -> when (cause.responseCode) {
                404 -> "电台地址失效，请尝试其他电台。"
                403 -> "电台拒绝连接，可能受地域限制。"
                else -> "电台暂时无法连接，请稍后重试。"
            }
            cause is IOException -> "网络连接失败，请检查网络后重试。"
            error?.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error?.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "网络连接失败，请检查网络后重试。"
            else -> "电台暂时无法连接，请稍后重试。"
        }
    }

    /**
     * MediaSession 回调：控制器传入的 MediaItem 若丢失 URI
     * （跨进程安全策略），从 extras 中还原流地址。
     */
    private inner class SessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                if (item.localConfiguration != null) {
                    item
                } else {
                    val station = MediaItemMapper.toStation(item)
                    if (station != null && station.streamUrl.isNotEmpty()) {
                        MediaItemMapper.toMediaItem(station)
                    } else {
                        item
                    }
                }
            }
            return Futures.immediateFuture(resolved.toMutableList())
        }
    }

    private fun getActivityPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConstants.AUDIO_NOTIFICATION_CHANNEL_ID,
                AppConstants.AUDIO_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        reconnect?.clear()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private companion object {
        /** 重连最大次数（指数退避：1s/2s/4s/8s） */
        const val MAX_RECONNECT = 4
    }
}
