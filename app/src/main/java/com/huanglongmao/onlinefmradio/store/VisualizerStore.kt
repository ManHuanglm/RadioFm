package com.huanglongmao.onlinefmradio.store

import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 可视化样式 */
enum class VisualizerStyle(val value: String, val label: String) {
    BARS("bars", "柱状"),
    LINES("lines", "线条"),
    PARTICLES("particles", "粒子");

    companion object {
        fun fromValue(v: String?): VisualizerStyle =
            entries.firstOrNull { it.value == v } ?: BARS
    }
}

/**
 * 动效设置服务（对应 Flutter 版 visualizer_settings_service.dart）。
 * 开关默认关闭；样式与速度系数持久化。
 */
class VisualizerStore(private val settings: SettingsDataStore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    private val _style = MutableStateFlow(VisualizerStyle.BARS)
    val style: StateFlow<VisualizerStyle> = _style

    private val _speedFactor = MutableStateFlow(1.0f)
    val speedFactor: StateFlow<Float> = _speedFactor

    /** 启动时加载 */
    suspend fun load() {
        _isEnabled.value = settings.getBool(AppConstants.KEY_VISUALIZER_ENABLED) ?: false
        _style.value = VisualizerStyle.fromValue(settings.getString(AppConstants.KEY_VISUALIZER_STYLE))
        _speedFactor.value = settings.getFloat(AppConstants.KEY_VISUALIZER_SPEED) ?: 1.0f
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        scope.launch { runCatching { settings.putBool(AppConstants.KEY_VISUALIZER_ENABLED, enabled) } }
    }

    fun setStyle(style: VisualizerStyle) {
        _style.value = style
        scope.launch { runCatching { settings.putString(AppConstants.KEY_VISUALIZER_STYLE, style.value) } }
    }

    /** 速度系数限制在 0.5 ~ 2.0 */
    fun setSpeedFactor(factor: Float) {
        val clamped = factor.coerceIn(0.5f, 2.0f)
        _speedFactor.value = clamped
        scope.launch { runCatching { settings.putFloat(AppConstants.KEY_VISUALIZER_SPEED, clamped) } }
    }

    init {
        scope.launch { load() }
    }
}
