package com.huanglongmao.onlinefmradio.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.core.theme.GradientThemes
import com.huanglongmao.onlinefmradio.core.util.BatteryOptimizationUtils
import com.huanglongmao.onlinefmradio.store.ThemeMode
import com.huanglongmao.onlinefmradio.store.VisualizerStyle
import com.huanglongmao.onlinefmradio.store.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 设置页（对应 Flutter 版 settings_page.dart）。
 * 分组卡片布局：播放 / 外观 / 通用 三组；外观含主题模式、字体大小、渐变主题。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeMode by container.themeStore.themeMode.collectAsStateWithLifecycle()
    val wallpaperIndex by container.themeStore.wallpaperIndex.collectAsStateWithLifecycle()
    val fontScale by container.themeStore.fontScale.collectAsStateWithLifecycle()
    val visualizerEnabled by container.visualizerStore.isEnabled.collectAsStateWithLifecycle()
    val visualizerStyle by container.visualizerStore.style.collectAsStateWithLifecycle()
    val visualizerSpeed by container.visualizerStore.speedFactor.collectAsStateWithLifecycle()
    val volume by container.playerController.volume.collectAsStateWithLifecycle()
    val favorites by container.favoritesStore.favorites.collectAsStateWithLifecycle()

    // 播放行为开关（DataStore 直读直写）
    val autoPlayKey = remember { booleanPreferencesKey(AppConstants.KEY_AUTO_PLAY_LAST) }
    val autoCollapseKey = remember { booleanPreferencesKey(AppConstants.KEY_MINI_AUTO_COLLAPSE) }
    val autoPlay by remember(autoPlayKey) {
        context.dataStore.data.map { it[autoPlayKey] ?: false }
    }.collectAsStateWithLifecycle(initialValue = false)
    val autoCollapse by remember(autoCollapseKey) {
        context.dataStore.data.map { it[autoCollapseKey] ?: true }
    }.collectAsStateWithLifecycle(initialValue = true)

    val batteryOk = BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // ===== 播放 =====
            SectionHeader("播放")
            SettingsCard {
                ToggleItem(
                    title = "打开 App 后自动播放上次节目",
                    subtitle = "关闭时仅恢复至底部播放条，不自动出声",
                    checked = autoPlay,
                    onChange = { v -> scope.launch { context.dataStore.edit { it[autoPlayKey] = v } } },
                )
                ItemDivider()
                ToggleItem(
                    title = "自动收缩播放状态栏",
                    subtitle = "展开后 10 秒内无操作则收缩为封面圆",
                    checked = autoCollapse,
                    onChange = { v -> scope.launch { context.dataStore.edit { it[autoCollapseKey] = v } } },
                )
                ItemDivider()
                NavItem(
                    title = "播放历史",
                    subtitle = "查看最近播放的节目",
                    onClick = { onNavigate(com.huanglongmao.onlinefmradio.ui.Routes.HISTORY) },
                )
                ItemDivider()
                ToggleItem(
                    title = "音乐可视化",
                    checked = visualizerEnabled,
                    onChange = { container.visualizerStore.setEnabled(it) },
                )
                if (visualizerEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        VisualizerStyle.entries.forEach { style ->
                            FilterChip(
                                selected = visualizerStyle == style,
                                onClick = { container.visualizerStore.setStyle(style) },
                                label = { Text(style.label) },
                            )
                        }
                    }
                    Text(
                        "动效速度 ×${"%.1f".format(visualizerSpeed)}",
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = visualizerSpeed,
                        onValueChange = { container.visualizerStore.setSpeedFactor(it) },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                ItemDivider()
                // 播放音量
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("播放音量", modifier = Modifier.weight(1f))
                    Text(
                        "${(volume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                com.huanglongmao.onlinefmradio.ui.components.SlimSlider(
                    value = volume,
                    onValueChange = { container.playerController.setVolume(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(6.dp))
            }

            // ===== 外观 =====
            SectionHeader("外观")
            SettingsCard {
                // 主题模式：跟随系统 / 浅色 / 深色（图标分段）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("主题模式", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = when (themeMode) {
                                ThemeMode.SYSTEM -> "跟随系统"
                                ThemeMode.LIGHT -> "浅色"
                                ThemeMode.DARK -> "深色"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ModeSegmented(
                        selected = themeMode,
                        onSelect = { scope.launch { container.themeStore.setThemeMode(it) } },
                    )
                }
                ItemDivider()
                // 字体大小：标准 / 大 / 特大
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("字体大小", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "当前：${fontScaleLabel(fontScale)}，影响应用内主要文字",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    TextSegmented(
                        options = listOf("标准" to 1f, "大" to 1.15f, "特大" to 1.3f),
                        selectedValue = fontScale,
                        onSelect = { scope.launch { container.themeStore.setFontScale(it) } },
                    )
                }
                ItemDivider()
                // 渐变主题：一行平铺圆形小色块
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("渐变主题", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GradientThemes.presets.forEachIndexed { index, theme ->
                            val selected = wallpaperIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = theme.name },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            Brush.verticalGradient(theme.gradient.takeLast(2)),
                                            CircleShape,
                                        )
                                        .border(
                                            width = if (selected) 2.dp else 0.dp,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.Transparent
                                            },
                                            shape = CircleShape,
                                        )
                                        .clickable {
                                            scope.launch { container.themeStore.setWallpaperIndex(index) }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = theme.name,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = GradientThemes.resolve(wallpaperIndex).name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ===== 通用 =====
            SectionHeader("通用")
            SettingsCard {
                NavItem(
                    title = "后台播放保障",
                    subtitle = if (batteryOk) {
                        "✓ 已加入电池优化白名单"
                    } else {
                        "允许忽略电池优化，避免后台播放被系统断网（国产 ROM 建议开启）"
                    },
                    subtitleColor = if (batteryOk) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    showChevron = !batteryOk,
                    onClick = {
                        if (!batteryOk) {
                            BatteryOptimizationUtils.requestIgnoreBatteryOptimizations(
                                context as androidx.activity.ComponentActivity,
                            )
                        }
                    },
                )
                ItemDivider()
                NavItem(
                    title = "导出收藏",
                    subtitle = "收藏列表导出为 favorites.json（当前 ${favorites.size} 条）",
                    onClick = {
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
                    },
                )
                ItemDivider()
                NavItem(
                    title = "数据更新",
                    subtitle = "全量更新 / 增量同步电台数据",
                    onClick = { onNavigate(com.huanglongmao.onlinefmradio.ui.Routes.STATION_UPDATE) },
                )
                ItemDivider()
                NavItem(
                    title = "开发者选项",
                    subtitle = "运行日志 / 数据导入导出",
                    onClick = { onNavigate(com.huanglongmao.onlinefmradio.ui.Routes.DEVELOPER) },
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun fontScaleLabel(scale: Float): String = when {
    scale >= 1.3f -> "特大"
    scale >= 1.15f -> "大"
    else -> "标准"
}

// ===== 分组卡片通用组件 =====

/** 分组标题（卡片外） */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp),
    )
}

/** 卡片容器 */
@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                RoundedCornerShape(16.dp),
            ),
        content = content,
    )
}

/** 条目间细分隔线 */
@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

/** 开关条目 */
@Composable
private fun ToggleItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** 导航条目 */
@Composable
private fun NavItem(
    title: String,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                )
            }
        }
        if (showChevron) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 主题模式图标分段控件（跟随系统 A / 深色月亮 / 浅色太阳） */
@Composable
private fun ModeSegmented(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options: List<Triple<ThemeMode, String?, ImageVector?>> = listOf(
        Triple(ThemeMode.SYSTEM, "A", null),
        Triple(ThemeMode.DARK, null, Icons.Filled.DarkMode),
        Triple(ThemeMode.LIGHT, null, Icons.Filled.LightMode),
    )
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                RoundedCornerShape(20.dp),
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (mode, label, icon) ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape,
                    )
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center,
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                } else if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = when (mode) {
                            ThemeMode.DARK -> "深色"
                            else -> "浅色"
                        },
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

/** 文字分段控件（等宽平分，选中项填充主题色圆角块） */
@Composable
private fun TextSegmented(
    options: List<Pair<String, Float>>,
    selectedValue: Float,
    onSelect: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                RoundedCornerShape(12.dp),
            )
            .padding(3.dp),
    ) {
        options.forEach { (label, value) ->
            val isSelected = kotlin.math.abs(selectedValue - value) < 0.01f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
