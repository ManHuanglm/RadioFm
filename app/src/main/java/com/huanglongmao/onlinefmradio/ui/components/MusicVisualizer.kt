package com.huanglongmao.onlinefmradio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.huanglongmao.onlinefmradio.store.VisualizerStyle
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 音乐可视化动效（对应 Flutter 版 music_visualizer.dart）。
 * 程序化动画（正弦叠加伪随机），不采集音频频谱，播放中才有律动。
 */
@Composable
fun MusicVisualizer(
    style: VisualizerStyle,
    isPlaying: Boolean,
    speedFactor: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    // 帧驱动：60fps 时间轴（秒），speedFactor 缩放节奏
    val time by produceState(0f, isPlaying, speedFactor) {
        var t = 0f
        val step = 16L
        while (true) {
            if (isPlaying) t += step / 1000f * speedFactor
            value = t
            delay(step)
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        when (style) {
            VisualizerStyle.BARS -> {
                val barCount = 24
                val barWidth = w / (barCount * 1.6f)
                for (i in 0 until barCount) {
                    val seed = i * 1.7f
                    val level = if (isPlaying) {
                        (abs(sin(time * 2.4f + seed)) * 0.6f + abs(sin(time * 1.1f + seed * 2.3f)) * 0.4f)
                            .coerceIn(0.08f, 1f)
                    } else 0.08f
                    val barH = h * level
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(i * (barWidth * 1.6f) + barWidth * 0.3f, h - barH),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(barWidth / 2),
                        style = Fill,
                    )
                }
            }

            VisualizerStyle.LINES -> {
                val lineCount = 3
                for (l in 0 until lineCount) {
                    val phase = time * (1.2f + l * 0.35f) + l * 2.1f
                    val path = Path()
                    val steps = 48
                    for (i in 0..steps) {
                        val x = w * i / steps
                        val base = if (isPlaying) {
                            (sin(i * 0.55f + phase) * 0.5f + sin(i * 0.23f + phase * 1.7f) * 0.5f)
                        } else 0f
                        val y = h / 2 + base * h * 0.42f
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path,
                        color = color.copy(alpha = 1f - l * 0.25f),
                        style = Stroke(width = (2.5f - l * 0.6f).dp.toPx()),
                    )
                }
            }

            VisualizerStyle.PARTICLES -> {
                val count = 26
                val cx = w / 2
                val cy = h / 2
                for (i in 0 until count) {
                    val seed = i * 2.399f
                    val angle = seed + (if (isPlaying) time * (0.6f + (i % 5) * 0.12f) else 0f)
                    val radiusBase = (i % 7 + 1) / 8f
                    val radius = if (isPlaying) {
                        radiusBase * h * 0.55f * (0.85f + 0.15f * sin(time * 3f + seed))
                    } else radiusBase * h * 0.4f
                    val x = cx + cos(angle) * radius
                    val y = cy + sin(angle) * radius * 0.8f
                    val r = (1.6f + (i % 4)) * if (isPlaying) 1.2f else 0.8f
                    drawCircle(
                        color = color.copy(alpha = if (isPlaying) 0.9f else 0.45f),
                        radius = r.dp.toPx() / 2,
                        center = Offset(x, y),
                        style = Fill,
                    )
                }
            }
        }
    }
}
