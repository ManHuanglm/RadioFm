package com.huanglongmao.onlinefmradio.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.core.theme.GradientThemes
import com.huanglongmao.onlinefmradio.store.VisualizerStyle
import com.huanglongmao.onlinefmradio.ui.components.MusicVisualizer
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

    val history by container.historyStore.history.collectAsStateWithLifecycle()
    val playHistory = playAction()

    val theme = GradientThemes.resolve(themeIndex)
    val isFav = station != null && favIds.contains(station!!.id)

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
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "收起",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = Color.White,
                ),
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp, bottom = padding.calculateBottomPadding() + 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 台标
                station?.let { StationLogo(station = it, size = 160.dp, cornerRadius = 28.dp) }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = station?.name ?: "未在播放",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
                Text(
                    text = station?.let { "${it.flagEmoji} ${it.country} · ${it.category}" }
                        ?: "选择一个电台开始收听",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )

                // 可视化动效
                if (visualizerEnabled) {
                    Spacer(Modifier.height(18.dp))
                    MusicVisualizer(
                        style = visualizerStyle,
                        isPlaying = isPlaying,
                        speedFactor = visualizerSpeed,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 36.dp),
                    )
                }

                Spacer(Modifier.weight(1f))

                // 状态提示：缓冲 / 重连 / 错误
                when {
                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFFFCDD2),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                        Spacer(Modifier.height(8.dp))
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

                Spacer(Modifier.weight(1f))

                // 播放控制区
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    IconButton(onClick = { controller.skipToPrevious() }) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = "上一台",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(42.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(onClick = { controller.togglePlayPause() }) {
                            if (isBuffering) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = when {
                                        isPlaying -> Icons.Filled.Pause
                                        station != null -> Icons.Filled.PlayArrow
                                        else -> Icons.Filled.Stop
                                    },
                                    contentDescription = "播放/暂停",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                        }
                    }
                    IconButton(onClick = { controller.skipToNext() }) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = "下一台",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {
                        station?.let { s -> scope.launch { container.favoritesStore.toggle(s) } }
                    }) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (isFav) Color(0xFFF87171) else Color.White,
                        )
                    }
                    IconButton(onClick = { controller.stop() }) {
                        Icon(Icons.Filled.Stop, contentDescription = "停止", tint = Color.White)
                    }
                    IconButton(onClick = { showSleepDialog = true }) {
                        Icon(Icons.Filled.Timer, contentDescription = "睡眠定时", tint = Color.White)
                    }
                }
                timerRemaining?.let { remaining ->
                    Text(
                        text = "定时关闭：${remaining.inWholeMinutes}分${remaining.inWholeSeconds % 60}秒",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                // 音量
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeDown,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Slider(
                        value = volume,
                        onValueChange = { controller.setVolume(it) },
                        modifier = Modifier.weight(1f),
                        // 细轨道样式（默认 M3 轨道过粗）
                        track = { state ->
                            val range = state.valueRange.endInclusive - state.valueRange.start
                            val fraction =
                                if (range > 0f) ((state.value - state.valueRange.start) / range)
                                    .coerceIn(0f, 1f) else 0f
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(Color.White.copy(alpha = 0.3f)),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(fraction)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(Color.White.copy(alpha = 0.85f)),
                                )
                            }
                        },
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White.copy(alpha = 0.85f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        ),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    }

    // 上滑弹出的最近播放列表
    if (showHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
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
                        TextButton(onClick = { scope.launch { container.historyStore.clear() } }) {
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
                                modifier = Modifier.clickable {
                                    playHistory(st)
                                    showHistorySheet = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
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
