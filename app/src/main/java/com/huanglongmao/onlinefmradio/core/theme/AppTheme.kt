package com.huanglongmao.onlinefmradio.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.huanglongmao.onlinefmradio.store.ThemeMode

/**
 * 应用主题（对应 Flutter 版 app_theme.dart 的 colorSchemeSeed 动态取色）。
 * 主题色由当前渐变主题的种子色派生。
 */
private fun lightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    secondary = seed.copy(alpha = 0.8f),
)

private fun darkScheme(seed: Color) = darkColorScheme(
    primary = seed,
    secondary = seed.copy(alpha = 0.8f),
)

@Composable
fun OnlineFmRadioTheme(
    themeMode: ThemeMode,
    wallpaperIndex: Int,
    content: @Composable () -> Unit,
) {
    val gradient = GradientThemes.resolve(wallpaperIndex)
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) darkScheme(gradient.seedColor) else lightScheme(gradient.seedColor),
        content = content,
    )
}
