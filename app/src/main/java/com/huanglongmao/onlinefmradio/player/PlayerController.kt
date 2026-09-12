package com.huanglongmao.onlinefmradio.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.util.AppLogger
import com.huanglongmao.onlinefmradio.data.model.Station
import com.huanglongmao.onlinefmradio.store.HistoryStore
import com.huanglongmao.onlinefmradio.store.SettingsDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.guava.await

/**
 * 播放器控制器（对应 Flutter 版 player_service.dart + audio_player_singleton.dart）。
 *
 * - 进程内单例，封装 MediaController；UI 只读取本类暴露的 StateFlow
 * - 统一播放入口（队列 / 单台）、循环切台、音量持久化、历史记录
 * - 重连状态与错误来自 PlayerEventBus（服务层写入）
 */
class PlayerController(
    private val context: Context,
    private val historyStore: HistoryStore,
    private val settings: SettingsDataStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val buildMutex = Mutex()
    private var controller: MediaController? = null

    // ===== UI 状态（对齐原 PlayerService 的状态字段）=====

    private val _currentStation = kotlinx.coroutines.flow.MutableStateFlow<Station?>(null)
    val currentStation: kotlinx.coroutines.flow.StateFlow<Station?> = _currentStation

    private val _isPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPlaying

    private val _isBuffering = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isBuffering: kotlinx.coroutines.flow.StateFlow<Boolean> = _isBuffering

    /** 是否处于自动重连中（UI 显示"重连中…"） */
    val isReconnecting = PlayerEventBus.reconnecting

    private val _errorMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val errorMessage: kotlinx.coroutines.flow.StateFlow<String?> = _errorMessage

    private val _volume = kotlinx.coroutines.flow.MutableStateFlow(AppConstants.DEFAULT_VOLUME)
    val volume: kotlinx.coroutines.flow.StateFlow<Float> = _volume

    init {
        // 恢复上次保存的音量
        scope.launch {
            _volume.value = settings.getFloat(AppConstants.KEY_VOLUME) ?: AppConstants.DEFAULT_VOLUME
        }
        // 重连耗尽后的友好错误 → UI 提示与手动重试
        scope.launch {
            PlayerEventBus.errors.collect { message ->
                AppLogger.e(TAG, "播放错误：$message")
                _errorMessage.value = message
                _isBuffering.value = false
                _isPlaying.value = false
            }
        }
        // 预热 MediaController
        scope.launch { runCatching { awaitController() } }
    }

    /** 构建 / 获取 MediaController（线程安全，幂等） */
    private suspend fun awaitController(): MediaController {
        controller?.let { return it }
        return buildMutex.withLock {
            controller?.let { return it }
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val c = MediaController.Builder(context, token).buildAsync().await()
            attach(c)
            c.volume = _volume.value
            controller = c
            c
        }
    }

    private fun attach(c: MediaController) {
        c.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // 线控 / 蓝牙 / 锁屏触发的切台也要同步 UI 并记录历史
                val station = MediaItemMapper.toStation(mediaItem) ?: return
                if (station.id != _currentStation.value?.id) {
                    _currentStation.value = station
                    _errorMessage.value = null
                    scope.launch { historyStore.addToHistory(station) }
                }
            }
        })
    }

    // ===== 播放控制 =====

    /** 播放指定电台 */
    fun play(station: Station) {
        scope.launch {
            AppLogger.i(TAG, "开始播放：${station.name}")
            _errorMessage.value = null
            _currentStation.value = station
            _isBuffering.value = true
            historyStore.addToHistory(station)
            runCatching {
                val c = awaitController()
                c.setMediaItem(MediaItemMapper.toMediaItem(station), 0)
                c.prepare()
                c.play()
            }.onFailure {
                PlayerEventBus.tryEmitError(friendlyError(it))
                _isBuffering.value = false
            }
        }
    }

    /** 使用播放队列播放，支持线控 / 蓝牙切台 */
    fun playFromQueue(stations: List<Station>, startIndex: Int) {
        if (startIndex < 0 || startIndex >= stations.size) return
        scope.launch {
            val station = stations[startIndex]
            _errorMessage.value = null
            _currentStation.value = station
            _isBuffering.value = true
            historyStore.addToHistory(station)
            runCatching {
                val c = awaitController()
                // 队列过大可能超出 Binder 事务限制，截断保护
                val from = maxOf(0, startIndex - 100)
                val to = minOf(stations.size, startIndex + 200)
                val items = stations.subList(from, to).map { MediaItemMapper.toMediaItem(it) }
                c.setMediaItems(items, startIndex - from, 0)
                c.prepare()
                c.play()
            }.onFailure {
                PlayerEventBus.tryEmitError(friendlyError(it))
                _isBuffering.value = false
            }
        }
    }

    fun pause() {
        scope.launch { runCatching { awaitController().pause() } }
    }

    fun resume() {
        scope.launch {
            if (_currentStation.value == null) return@launch
            runCatching {
                val c = awaitController()
                // 无已加载源（长时间暂停后连接被回收）时重载当前电台
                if (c.playbackState == Player.STATE_IDLE || c.mediaItemCount == 0) {
                    _currentStation.value?.let { station ->
                        c.setMediaItem(MediaItemMapper.toMediaItem(station), 0)
                        c.prepare()
                    }
                }
                c.play()
            }
        }
    }

    /** 通知栏 / 迷你播放器的播放暂停切换 */
    fun togglePlayPause() {
        if (_isPlaying.value) pause() else resume()
    }

    fun stop() {
        scope.launch {
            AppLogger.i(TAG, "停止播放：${_currentStation.value?.name.orEmpty()}")
            runCatching {
                val c = awaitController()
                c.stop()
                c.clearMediaItems()
            }
            _currentStation.value = null
            _isPlaying.value = false
            _isBuffering.value = false
            _errorMessage.value = null
        }
    }

    /** 重试当前电台（错误提示上的"重试"按钮） */
    fun retry() {
        val station = _currentStation.value ?: return
        _errorMessage.value = null
        _isBuffering.value = true
        play(station)
    }

    fun skipToNext() = skipBy(1)

    fun skipToPrevious() = skipBy(-1)

    /** 循环切台（对齐原版取模逻辑） */
    private fun skipBy(delta: Int) {
        scope.launch {
            runCatching {
                val c = awaitController()
                val count = c.mediaItemCount
                if (count <= 0) return@runCatching
                val next = (c.currentMediaItemIndex + delta).mod(count)
                c.seekTo(next, 0)
                c.prepare()
                c.play()
            }
        }
    }

    // ===== 音量 =====

    /** 设置音量并持久化，重启后保持 */
    fun setVolume(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _volume.value = clamped
        scope.launch {
            runCatching { awaitController().volume = clamped }
            runCatching { settings.putFloat(AppConstants.KEY_VOLUME, clamped) }
        }
    }

    /** 睡眠定时淡出：临时缩放音量，不持久化（对齐原版 applySleepFade） */
    fun applySleepFade(scale: Float) {
        scope.launch {
            runCatching {
                awaitController().volume = (_volume.value * scale).coerceIn(0f, 1f)
            }
        }
    }

    /** 恢复正常音量（取消睡眠定时时调用） */
    fun restoreVolume() {
        scope.launch {
            runCatching { awaitController().volume = _volume.value }
        }
    }

    /** 将启动失败等异常映射为友好文案 */
    private fun friendlyError(t: Throwable): String {
        val message = (t.cause ?: t).message?.lowercase().orEmpty()
        return when {
            message.contains("404") || message.contains("not found") -> "电台地址失效，请尝试其他电台。"
            message.contains("403") || message.contains("forbidden") -> "电台拒绝连接，可能受地域限制。"
            else -> "电台暂时无法连接，请稍后重试。"
        }
    }

    private companion object {
        const val TAG = "Player"
    }
}
