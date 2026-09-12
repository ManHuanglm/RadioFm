package com.huanglongmao.onlinefmradio.player

import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.store.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 睡眠定时服务（对应 Flutter 版 sleep_timer_service.dart）。
 *
 * - 每秒更新剩余时间
 * - 进入淡出窗口（默认 20s）后按剩余时间线性下发音量缩放系数（不持久化）
 * - 归零后回调 onComplete（通常为停止播放）
 */
class SleepTimerManager(
    private val player: PlayerController,
    private val settings: SettingsDataStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    private val _remaining = MutableStateFlow<Duration?>(null)
    val remaining: StateFlow<Duration?> = _remaining

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted

    /** 上次选择的分钟数（设置默认值用），-1 表示未设置 */
    private val _selectedMinutes = MutableStateFlow(AppConstants.DEFAULT_SLEEP_TIMER_MINUTES)
    val selectedMinutes: StateFlow<Int> = _selectedMinutes

    init {
        scope.launch {
            _selectedMinutes.value =
                settings.getInt(AppConstants.KEY_SLEEP_TIMER)
                    ?: AppConstants.DEFAULT_SLEEP_TIMER_MINUTES
        }
    }

    /** 启动定时关闭；同时持久化所选分钟数 */
    fun start(duration: Duration, persistMinutes: Int? = null) {
        cancel(restoreVolume = true)
        _isCompleted.value = false
        _remaining.value = duration
        _isActive.value = true
        persistMinutes?.let { minutes ->
            _selectedMinutes.value = minutes
            scope.launch { runCatching { settings.putInt(AppConstants.KEY_SLEEP_TIMER, minutes) } }
        }
        job = scope.launch {
            var remaining = duration
            while (remaining > Duration.ZERO) {
                delay(1_000)
                remaining -= 1.seconds
                _remaining.value = remaining
                applyFade(remaining)
            }
            complete()
        }
    }

    /** 计算并下发渐弱缩放系数 */
    private fun applyFade(remaining: Duration) {
        if (remaining >= FADE_WINDOW) {
            player.applySleepFade(1.0f)
            return
        }
        val scale = (remaining.inWholeMilliseconds.toFloat() / FADE_WINDOW.inWholeMilliseconds)
            .coerceIn(0f, 1f)
        player.applySleepFade(scale)
    }

    /**
     * 取消定时关闭。
     * [restoreVolume] 取消时应恢复音量；正常完成时已淡到 0 由停止接管。
     */
    fun cancel(restoreVolume: Boolean = true) {
        val wasActive = _isActive.value
        job?.cancel()
        job = null
        _remaining.value = null
        _isActive.value = false
        if (restoreVolume && wasActive) {
            player.restoreVolume()
        }
    }

    private fun complete() {
        job?.cancel()
        job = null
        _remaining.value = Duration.ZERO
        _isActive.value = false
        _isCompleted.value = true
        // 定时结束：停止播放（对齐原版 onComplete → PlayerService.stop）
        player.stop()
    }

    companion object {
        /** 渐弱窗口时长（总时长不足时直接按比例淡出） */
        val FADE_WINDOW = 20.seconds
    }
}
