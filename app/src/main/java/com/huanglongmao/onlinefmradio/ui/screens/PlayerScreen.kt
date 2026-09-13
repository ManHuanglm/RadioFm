package com.huanglongmao.onlinefmradio.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.core.theme.FavoriteRed
import com.huanglongmao.onlinefmradio.core.theme.GradientThemes
import com.huanglongmao.onlinefmradio.core.theme.PlayerErrorText
import com.huanglongmao.onlinefmradio.data.model.Station
import com.huanglongmao.onlinefmradio.store.VisualizerStyle
import com.huanglongmao.onlinefmradio.ui.components.MusicVisualizer
import com.huanglongmao.onlinefmradio.ui.components.SlimSlider
import com.huanglongmao.onlinefmradio.ui.components.StationLogo
import com.huanglongmao.onlinefmradio.ui.components.playAction
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

/**
 * 播放页（对应 Flutter 版 player_page.dart）：
 * 渐变主题全屏背景 + 大 Logo + 可视化动效 + 播放控制 + 音量 + 睡眠定时 + 错误重试。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val controller = container.playerController
    val sleepTimer = container.sleepTimerManager
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val station by controller.currentStation.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by controller.isBuffering.collectAsStateWithLifecycle()
    val isReconnecting by controller.isReconnecting.collectAsStateWithLifecycle()
    val errorMessage by controller.errorMessage.collectAsStateWithLifecycle()
    val volume by controller.volume.collectAsStateWithLifecycle()
    val themeIndex by container.themeStore.wallpaperIndex.collectAsStateWithLifecycle()
    val favIds by container.favoritesStore.favoriteIds.collectAsStateWithLifecycle()
    val visualizerEnabled by container.visualizerStore.isEnabled.collectAsStateWithLifecycle()
    val visualizerStyle by container.visualizerStore.style.collectAsStateWithLifecycle()
    val visualizerSpeed by container.visualizerStore.speedFactor.collectAsStateWithLifecycle()
    val timerRemaining by sleepTimer.remaining.collectAsStateWithLifecycle()

    var showSleepDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showVolumeSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showStationInfo by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val history by container.historyStore.history.collectAsStateWithLifecycle()
    val playHistory = playAction()

    val theme = GradientThemes.resolve(themeIndex)
    // 一次性快照可空状态，避免委托属性二次读取 + !! 强制解包
    val st = station
    val isFav = st != null && favIds.contains(st.id)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(theme.backgroundBrush)
                // 上滑手势：弹出最近播放列表
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount < -8) showHistorySheet = true
                    }
                },
        ) {
            PlayerTopBar(
                title = station?.name ?: "播放",
                onBack = onBack,
                menuExpanded = showMoreMenu,
                onMenuExpandedChange = { showMoreMenu = it },
                onShare = {
                    showMoreMenu = false
                    station?.let { s -> shareStation(context, s) }
                },
                onShowInfo = {
                    showMoreMenu = false
                    if (station != null) showStationInfo = true
                },
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp, bottom = padding.calculateBottomPadding() + 14.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))

                // 国家信息（台标上方）
                Text(
                    text = station?.let { "${it.flagEmoji} ${it.country}" } ?: "",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(20.dp))

                // 大台标
                station?.let { StationLogo(station = it, size = 116.dp, cornerRadius = 15.dp) }

                Spacer(Modifier.weight(1.2f))

                // 可视化动效（开启时显示）
                if (visualizerEnabled) {
                    MusicVisualizer(
                        style = visualizerStyle,
                        isPlaying = isPlaying,
                        speedFactor = visualizerSpeed,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // 状态提示：缓冲 / 重连 / 错误
                val currentError = errorMessage
                when {
                    currentError != null -> {
                        Text(
                            text = currentError,
                            color = PlayerErrorText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { controller.retry() }) {
                            Text("点击重试", color = Color.White)
                        }
                    }
                    isReconnecting -> Text(
                        text = "重连中…",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    isBuffering -> CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                // 功能按钮排：音量 / 定时 / 收藏 / 动效
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CircleAction(icon = Icons.AutoMirrored.Filled.VolumeUp, label = "音量") {
                        showVolumeSheet = true
                    }
                    CircleAction(icon = Icons.Filled.Timer, label = "定时") {
                        showSleepDialog = true
                    }
                    CircleAction(
                        icon = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        label = "收藏",
                        iconTint = if (isFav) FavoriteRed else Color.White,
                    ) {
                        station?.let { s -> scope.launch { container.favoritesStore.toggle(s) } }
                    }
                    CircleAction(icon = Icons.Filled.Equalizer, label = "动效") {
                        container.visualizerStore.setEnabled(!visualizerEnabled)
                    }
                }
                timerRemaining?.let { remaining ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "定时关闭：${remaining.inWholeMinutes}分${remaining.inWholeSeconds % 60}秒",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                Spacer(Modifier.weight(1.2f))

                // 播放控制
                PlayControlsRow(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    hasStation = station != null,
                    onPrevious = { controller.skipToPrevious() },
                    onTogglePlay = { controller.togglePlayPause() },
                    onNext = { controller.skipToNext() },
                )

                Spacer(Modifier.weight(1f))

                // 上滑手势提示（短横线 + 文字）
                Box(
                    Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.4f)),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "上滑查看最近播放",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }

    // 音量调节弹层
    if (showVolumeSheet) {
        VolumeSheet(
            volume = volume,
            onVolumeChange = { controller.setVolume(it) },
            onDismiss = { showVolumeSheet = false },
        )
    }

    // 电台信息弹窗
    if (showStationInfo) {
        station?.let { s ->
            StationInfoDialog(station = s, onDismiss = { showStationInfo = false })
        }
    }

    // 上滑弹出的最近播放列表
    if (showHistorySheet) {
        HistorySheet(
            history = history,
            onPlay = {
                playHistory(it)
                showHistorySheet = false
            },
            onClear = { scope.launch { container.historyStore.clear() } },
            onDismiss = { showHistorySheet = false },
        )
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            selectedMinutes = sleepTimer.selectedMinutes.value,
            isActive = sleepTimer.isActive.value,
            onStart = { minutes ->
                sleepTimer.start(minutes.minutes, persistMinutes = minutes)
                showSleepDialog = false
            },
            onCancel = {
                sleepTimer.cancel()
                showSleepDialog = false
            },
            onDismiss = { showSleepDialog = false },
        )
    }
}

/** 播放页顶栏：标题 + 收起 + 更多菜单（分享 / 电台信息） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerTopBar(
    title: String,
    onBack: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onShare: () -> Unit,
    onShowInfo: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "收起",
                    tint = Color.White,
                )
            }
        },
        actions = {
            Box {
                IconButton(onClick = { onMenuExpandedChange(true) }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "更多",
                        tint = Color.White,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { onMenuExpandedChange(false) },
                ) {
                    DropdownMenuItem(text = { Text("分享电台") }, onClick = onShare)
                    DropdownMenuItem(text = { Text("电台信息") }, onClick = onShowInfo)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            navigationIconContentColor = Color.White,
        ),
    )
}

/** 系统分享电台（名称 + 流地址） */
private fun shareStation(context: Context, station: Station) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${station.name}\n${station.streamUrl}")
    }
    context.startActivity(Intent.createChooser(intent, "分享电台"))
}

/** 播放控制排：上一台 / 播放暂停 / 下一台 */
@Composable
private fun PlayControlsRow(
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasStation: Boolean,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = "上一台",
                tint = Color.White,
                modifier = Modifier.size(44.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(88.dp)
                .shadow(10.dp, RoundedCornerShape(44.dp))
                .clip(RoundedCornerShape(44.dp))
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onTogglePlay) {
                if (isBuffering) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = when {
                            isPlaying -> Icons.Filled.Pause
                            hasStation -> Icons.Filled.PlayArrow
                            else -> Icons.Filled.Stop
                        },
                        contentDescription = "播放/暂停",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = "下一台",
                tint = Color.White,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}

/** 音量调节弹层 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeSheet(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("音量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
                SlimSlider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 电台信息弹窗 */
@Composable
private fun StationInfoDialog(station: Station, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(station.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("国家", "${station.flagEmoji} ${station.country}")
                if (station.language.isNotBlank()) InfoRow("语言", station.language)
                InfoRow("类型", station.category)
                if (station.bitrate > 0) InfoRow("比特率", "${station.bitrate} kbps")
                if (station.votes > 0) InfoRow("投票", "${station.votes}")
                InfoRow("地址", station.streamUrl)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

/** 上滑弹出的最近播放列表 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    history: List<Station>,
    onPlay: (Station) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "最近播放",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("清空")
                    }
                }
            }
            if (history.isEmpty()) {
                Text(
                    "暂无播放记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            } else {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(history, key = { it.id }) { st ->
                        ListItem(
                            headlineContent = {
                                Text(st.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                Text("${st.flagEmoji} ${st.country}")
                            },
                            leadingContent = {
                                StationLogo(station = st, size = 44.dp, cornerRadius = 10.dp)
                            },
                            modifier = Modifier.clickable { onPlay(st) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** 睡眠定时选择对话框 */
@Composable
private fun SleepTimerDialog(
    selectedMinutes: Int,
    isActive: Boolean,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(15, 30, 45, 60, 90)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("睡眠定时") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("到时间后将停止播放，结束前音量会逐渐减弱。")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { minutes ->
                        TextButton(onClick = { onStart(minutes) }) {
                            Text(
                                text = "${minutes}分" + if (minutes == selectedMinutes) " ✓" else "",
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (isActive) {
                TextButton(onClick = onCancel) { Text("取消定时") }
            } else {
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
    )
}

/** 电台信息弹窗中的单行「标签：值」 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 圆形功能按钮 + 底部文字标签（对齐原版播放页功能排样式） */
@Composable
private fun CircleAction(
    icon: ImageVector,
    label: String,
    iconTint: Color = Color.White,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
