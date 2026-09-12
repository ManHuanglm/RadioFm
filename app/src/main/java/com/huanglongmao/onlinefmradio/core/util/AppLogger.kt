package com.huanglongmao.onlinefmradio.core.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 应用内存日志（开发者功能）：环形缓冲最近 N 条，供开发者页面查看/复制。
 * 每条同时输出到 Logcat。
 */
object AppLogger {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    data class LogEntry(
        val time: Long,
        val level: Level,
        val tag: String,
        val message: String,
    ) {
        private val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

        override fun toString(): String =
            "${fmt.format(Date(time))} [${level.name}] $tag: $message"
    }

    private const val MAX_ENTRIES = 500

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())

    /** 当前日志快照（新在后） */
    val entries: StateFlow<List<LogEntry>> = _entries

    fun d(tag: String, message: String) = log(Level.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(Level.INFO, tag, message)
    fun w(tag: String, message: String) = log(Level.WARN, tag, message)
    fun e(tag: String, message: String) = log(Level.ERROR, tag, message)

    /** 记录一条日志：进内存环形缓冲 + Logcat */
    fun log(level: Level, tag: String, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), level, tag, message)
        _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
        when (level) {
            Level.DEBUG -> Log.d(tag, message)
            Level.INFO -> Log.i(tag, message)
            Level.WARN -> Log.w(tag, message)
            Level.ERROR -> Log.e(tag, message)
        }
    }

    /** 清空日志 */
    fun clear() {
        _entries.value = emptyList()
    }
}
