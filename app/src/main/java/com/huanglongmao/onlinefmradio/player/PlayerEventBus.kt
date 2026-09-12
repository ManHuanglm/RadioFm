package com.huanglongmao.onlinefmradio.player

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 播放事件总线（进程内单例）。
 *
 * UI 与 MediaSessionService 同进程运行，通过该对象传递：
 * - 自动重连中状态（UI 显示"重连中…"）
 * - 重连耗尽 / 致命错误的友好文案
 *
 * 对应 Flutter 版 RadioAudioHandler 的 reconnectingStream / errorStream。
 */
object PlayerEventBus {

    /** 是否正处于自动重连中 */
    val reconnecting = MutableStateFlow(false)

    /** 友好错误消息流 */
    val errors = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun tryEmitError(message: String) {
        errors.tryEmit(message)
    }

    fun reset() {
        reconnecting.value = false
    }
}
