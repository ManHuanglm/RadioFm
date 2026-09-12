package com.huanglongmao.onlinefmradio.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.core.theme.GradientThemes
import com.huanglongmao.onlinefmradio.core.util.BatteryOptimizationUtils
import com.huanglongmao.onlinefmradio.store.ThemeMode
import com.huanglongmao.onlinefmradio.store.VisualizerStyle
import kotlinx.coroutines.launch

/**
 * 设置页（对应 Flutter 版 settings_page.dart）：
 * 主题模式 / 8 套渐变主题 / 可视化动效 / 音量 / 电池优化 / 导出缓存。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeMode by container.themeStore.themeMode.collectAsStateWithLifecycle()
    val wallpaperIndex by container.themeStore.wallpaperIndex.collectAsStateWithLifecycle()
    val visualizerEnabled by container.visualizerStore.isEnabled.collectAsStateWithLifecycle()
    val visualizerStyle by container.visualizerStore.style.collectAsStateWithLifecycle()
    val visualizerSpeed by container.visualizerStore.speedFactor.collectAsStateWithLifecycle()
    val volume by container.playerController.volume.collectAsStateWithLifecycle()
    val favorites by container.favoritesStore.favorites.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 主题模式
            Text("主题模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ThemeMode.SYSTEM to "跟随系统",
                    ThemeMode.LIGHT to "浅色",
                    ThemeMode.DARK to "深色",
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { scope.launch { container.themeStore.setThemeMode(mode) } },
                        label = { Text(label) },
                    )
                }
            }

            // 渐变主题网格
            Text("渐变主题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientThemes.presets.chunked(2).forEachIndexed { rowIdx, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEachIndexed { colIdx, theme ->
                            val index = rowIdx * 2 + colIdx
                            val selected = wallpaperIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .background(
                                        Brush.linearGradient(theme.gradient),
                                        RoundedCornerShape(14.dp),
                                    )
                                    .clickable {
                                        scope.launch { container.themeStore.setWallpaperIndex(index) }
                                    }
                                    .padding(10.dp),
                            ) {
                                Text(
                                    text = theme.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                if (selected) {
                                    Text(
                                        text = "✓",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 可视化动效
            Text("播放动效", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("音乐可视化", modifier = Modifier.weight(1f))
                Switch(
                    checked = visualizerEnabled,
                    onCheckedChange = { container.visualizerStore.setEnabled(it) },
                )
            }
            if (visualizerEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisualizerStyle.entries.forEach { style ->
                        FilterChip(
                            selected = visualizerStyle == style,
                            onClick = { container.visualizerStore.setStyle(style) },
                            label = { Text(style.label) },
                        )
                    }
                }
                Text("动效速度 ×${"%.1f".format(visualizerSpeed)}")
                Slider(
                    value = visualizerSpeed,
                    onValueChange = { container.visualizerStore.setSpeedFactor(it) },
                    valueRange = 0.5f..2.0f,
                )
            }

            // 音量
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                Text("  播放音量", modifier = Modifier.weight(1f))
                Text("${(volume * 100).toInt()}%")
            }
            Slider(
                value = volume,
                onValueChange = { container.playerController.setVolume(it) },
                valueRange = 0f..1f,
            )

            // 电池优化引导
            Text("后台播放保障", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "允许忽略电池优化可避免后台播放被系统断网杀进程（国产 ROM 建议开启）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context)) {
                Text("✓ 已加入电池优化白名单", color = MaterialTheme.colorScheme.primary)
            } else {
                Button(onClick = {
                    BatteryOptimizationUtils.requestIgnoreBatteryOptimizations(context as androidx.activity.ComponentActivity)
                }) { Text("去设置") }
            }

            // 导出收藏（开发者调试）
            Text("数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        val content = container.importExportManager.generateJson(favorites)
                        val file = java.io.File(context.getExternalFilesDir(null), "favorites.json")
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            file.writeText(content)
                        }
                        android.widget.Toast.makeText(
                            context, "已导出到：${file.absolutePath}", android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
                }) { Text("导出收藏") }
                Button(onClick = { onNavigate(com.huanglongmao.onlinefmradio.ui.Routes.STATION_UPDATE) }) {
                    Text("数据更新")
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
