package com.huanglongmao.onlinefmradio.store

import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.theme.GradientThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 主题模式 */
enum class ThemeMode(val value: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        fun fromValue(v: String?): ThemeMode =
            entries.firstOrNull { it.value == v } ?: SYSTEM
    }
}

/**
 * 主题服务（对应 Flutter 版 theme_service.dart）。
 * 管理：主题模式 + 渐变壁纸索引，均持久化。
 */
class ThemeStore(private val settings: SettingsDataStore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _wallpaperIndex = MutableStateFlow(GradientThemes.defaultIndex)
    val wallpaperIndex: StateFlow<Int> = _wallpaperIndex

    /** 字体缩放（标准 1.0 / 大 1.15 / 特大 1.3） */
    private val _fontScale = MutableStateFlow(1f)
    val fontScale: StateFlow<Float> = _fontScale

    /** 启动时加载 */
    suspend fun load() {
        _themeMode.value = ThemeMode.fromValue(settings.getString(AppConstants.KEY_THEME_MODE))
        val index = settings.getInt(AppConstants.KEY_GRADIENT_WALLPAPER_INDEX)
            ?: GradientThemes.defaultIndex
        _wallpaperIndex.value = GradientThemes.resolve(index).let { preset ->
            GradientThemes.presets.indexOf(preset).takeIf { it >= 0 } ?: GradientThemes.defaultIndex
        }
        _fontScale.value = settings.getFloat(AppConstants.KEY_FONT_SCALE) ?: 1f
    }

    /** 设置主题模式并持久化 */
    suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        runCatching { settings.putString(AppConstants.KEY_THEME_MODE, mode.value) }
    }

    /** 设置渐变壁纸索引（越界自动回退默认）并持久化 */
    suspend fun setWallpaperIndex(index: Int) {
        val safe = index.coerceIn(0, GradientThemes.presets.size - 1)
        _wallpaperIndex.value = safe
        runCatching { settings.putInt(AppConstants.KEY_GRADIENT_WALLPAPER_INDEX, safe) }
    }

    /** 设置字体缩放并持久化 */
    suspend fun setFontScale(scale: Float) {
        _fontScale.value = scale
        runCatching { settings.putFloat(AppConstants.KEY_FONT_SCALE, scale) }
    }

    init {
        scope.launch { load() }
    }
}
