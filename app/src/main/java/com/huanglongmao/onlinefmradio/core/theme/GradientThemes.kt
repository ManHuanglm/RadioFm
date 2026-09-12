package com.huanglongmao.onlinefmradio.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 渐变主题壁纸预设（精确移植 Flutter 版 gradient_themes.dart）。
 * 每套包含：名称 / 种子色 / 强调色 / 3 段背景渐变色。
 * 追加新主题只能放列表末尾，不能调整已有项顺序（持久化索引会错位）。
 */
@Immutable
data class GradientTheme(
    val name: String,
    val seedColor: Color,
    val accentColor: Color,
    val gradient: List<Color>,
) {
    /** 播放页背景（自上而下线性渐变） */
    val backgroundBrush: Brush
        get() = Brush.verticalGradient(colors = gradient)

    /** 对角渐变（用于分类卡片等装饰） */
    fun diagonalBrush(): Brush = Brush.linearGradient(
        colors = gradient,
        start = Offset.Zero,
        end = Offset.Infinite,
    )
}

object GradientThemes {
    /** 默认主题索引（0 = 靛蓝之夜，与历史版本保持一致） */
    const val defaultIndex = 0

    val presets: List<GradientTheme> = listOf(
        // 0. 靛蓝之夜（默认）
        GradientTheme(
            name = "靛蓝之夜",
            seedColor = Color(0xFF6366F1),
            accentColor = Color(0xFF6366F1),
            gradient = listOf(
                Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460),
            ),
        ),
        // 1. 紫罗兰梦境
        GradientTheme(
            name = "紫罗兰梦境",
            seedColor = Color(0xFF8B5CF6),
            accentColor = Color(0xFFA78BFA),
            gradient = listOf(
                Color(0xFF2D1B69), Color(0xFF11052C), Color(0xFF3A0CA3),
            ),
        ),
        // 2. 落日余晖
        GradientTheme(
            name = "落日余晖",
            seedColor = Color(0xFFF59E0B),
            accentColor = Color(0xFFFBBF24),
            gradient = listOf(
                Color(0xFF2D1B0E), Color(0xFF3E1F0F), Color(0xFF7F1D1D),
            ),
        ),
        // 3. 极光森林
        GradientTheme(
            name = "极光森林",
            seedColor = Color(0xFF10B981),
            accentColor = Color(0xFF34D399),
            gradient = listOf(
                Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF0B3D2E),
            ),
        ),
        // 4. 深海幽蓝
        GradientTheme(
            name = "深海幽蓝",
            seedColor = Color(0xFF3B82F6),
            accentColor = Color(0xFF60A5FA),
            gradient = listOf(
                Color(0xFF0F2027), Color(0xFF1A3A5C), Color(0xFF0B2545),
            ),
        ),
        // 5. 樱花初绽
        GradientTheme(
            name = "樱花初绽",
            seedColor = Color(0xFFEC4899),
            accentColor = Color(0xFFF472B6),
            gradient = listOf(
                Color(0xFF3D1E2E), Color(0xFF5C2640), Color(0xFF8B3A62),
            ),
        ),
        // 6. 暗夜玫瑰
        GradientTheme(
            name = "暗夜玫瑰",
            seedColor = Color(0xFFE11D48),
            accentColor = Color(0xFFFB7185),
            gradient = listOf(
                Color(0xFF1A0A12), Color(0xFF2A0E1B), Color(0xFF4A1230),
            ),
        ),
        // 7. 翡翠星河
        GradientTheme(
            name = "翡翠星河",
            seedColor = Color(0xFF14B8A6),
            accentColor = Color(0xFF2DD4BF),
            gradient = listOf(
                Color(0xFF0A1A2E), Color(0xFF0E2A3D), Color(0xFF0F4C3A),
            ),
        ),
    )

    /** 按索引取主题，越界回退默认 */
    fun resolve(index: Int): GradientTheme =
        presets.getOrElse(index) { presets[defaultIndex] }
}
