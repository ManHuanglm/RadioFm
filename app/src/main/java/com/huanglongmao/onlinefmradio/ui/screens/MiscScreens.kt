package com.huanglongmao.onlinefmradio.ui.screens

import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.data.model.ReleaseNote
import com.huanglongmao.onlinefmradio.data.model.Station
import com.huanglongmao.onlinefmradio.store.RecordingFile
import com.huanglongmao.onlinefmradio.store.RecordingState
import com.huanglongmao.onlinefmradio.ui.components.StationLogo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 外围页面集合：
 * - 录音页（对应 recording_page.dart，网络流直录 + 文件管理）
 * - 闹钟占位页（对应 alarm_page.dart，标注"开发中"）
 * - 更新日志页（对应 changelog_page.dart，GitHub Releases）
 * - 帮助页（对应 help_page.dart）
 */

/** 占位页通用骨架 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(title: String, emoji: String, message: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emoji, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(12.dp))
                Text(message, style = MaterialTheme.typography.titleMedium)
                Text(
                    "敬请期待后续版本",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 录音页：录制正在播放的电台流（无损转存）+ 录音文件管理 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = container.recordingManager
    val recordingState by manager.state.collectAsStateWithLifecycle()
    val currentStation by container.playerController.currentStation.collectAsStateWithLifecycle()
    var files by remember { mutableStateOf<List<RecordingFile>>(emptyList()) }
    var toast by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<RecordingFile?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun refresh() {
        scope.launch { files = manager.list() }
    }

    LaunchedEffect(Unit) { refresh() }

    // 录制结束（回到 Idle）时刷新文件列表
    LaunchedEffect(recordingState) {
        if (recordingState is RecordingState.Idle) refresh()
    }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            toast = null
        }
    }

    fun openRecording(file: RecordingFile) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(file.path),
        )
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "audio/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
            )
        }.onFailure { toast = "未找到可播放音频的应用" }
    }

    fun shareRecording(file: RecordingFile) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(file.path),
        )
        runCatching {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "分享录音",
                ),
            )
        }.onFailure { toast = "未找到可分享的应用" }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("录音") },
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
                .padding(horizontal = 16.dp),
        ) {
            RecordControlCard(
                currentStation = currentStation,
                state = recordingState,
                onStart = { currentStation?.let { if (!manager.start(it)) toast = "已在录制中" } },
                onStop = { manager.stop() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "录音文件",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            if (files.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "暂无录音，播放电台后点击上方按钮开始录制",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(files, key = { it.path }) { file ->
                        RecordingFileRow(
                            file = file,
                            onPlay = { openRecording(file) },
                            onShare = { shareRecording(file) },
                            onDelete = { pendingDelete = file },
                        )
                        HorizontalDivider()
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // 删除确认
    pendingDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除录音") },
            text = { Text("确定删除「${file.name}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch {
                        if (manager.delete(file.name)) refresh() else toast = "删除失败"
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

/** 录制控制卡片：当前电台 + 开始/停止录制 + 实时状态 */
@Composable
private fun RecordControlCard(
    currentStation: Station?,
    state: RecordingState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    // 录制中每 500ms 刷新一次已录时长
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state) {
        while (state is RecordingState.Recording) {
            delay(500)
            now = System.currentTimeMillis()
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            val station = currentStation
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (station != null) {
                    StationLogo(station = station, size = 52.dp, cornerRadius = 12.dp)
                } else {
                    Box(
                        Modifier
                            .size(52.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape,
                            ),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = currentStation?.name ?: "未在播放",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val st = state
                    Text(
                        text = when (st) {
                            is RecordingState.Recording ->
                                "● 录制中 ${formatDuration(now - st.startedAt)} · ${formatSize(st.bytes)}"
                            is RecordingState.Connecting -> "正在连接电台流…"
                            RecordingState.Idle -> "空闲，点击下方按钮开始录制"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (st) {
                            is RecordingState.Recording -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                if (state is RecordingState.Recording) {
                    // 呼吸红点
                    RecordPulsingDot(Modifier.size(10.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            val st = state
            Button(
                onClick = if (st is RecordingState.Idle) onStart else onStop,
                enabled = st is RecordingState.Recording ||
                    st is RecordingState.Connecting ||
                    currentStation != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (st is RecordingState.Idle) Icons.Filled.Mic else Icons.Filled.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = when (st) {
                        is RecordingState.Idle -> "开始录制"
                        is RecordingState.Connecting -> "取消"
                        is RecordingState.Recording -> "停止录制"
                    },
                )
            }
        }
    }
}

/** 录制中红点呼吸动画 */
@Composable
private fun RecordPulsingDot(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "recordDot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "recordDotAlpha",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.error.copy(alpha = alpha), CircleShape),
    )
}

/** 录音文件行：名称/大小/日期 + 播放/分享/删除 */
@Composable
private fun RecordingFileRow(
    file: RecordingFile,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = "播放",
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${formatSize(file.sizeBytes)} · " +
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(file.lastModified)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Filled.Share, contentDescription = "分享", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
        }
    }
}

/** 字节数转可读大小 */
private fun formatSize(bytes: Long): String = when {
    bytes >= 1 shl 20 -> "%.1f MB".format(Locale.US, bytes / 1048576.0)
    bytes >= 1 shl 10 -> "%.0f KB".format(Locale.US, bytes / 1024.0)
    else -> "$bytes B"
}

/** 毫秒转 mm:ss / hh:mm:ss */
private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(Locale.US, h, m, s) else "%02d:%02d".format(Locale.US, m, s)
}

/** 闹钟页（占位） */
@Composable
fun AlarmScreen(onBack: () -> Unit) =
    PlaceholderScreen("闹钟", "⏰", "电台闹钟功能开发中", onBack)

/** 更新日志页：从 GitHub Releases 拉取版本记录 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    var loading by remember { mutableStateOf(true) }
    var releases by remember { mutableStateOf<List<ReleaseNote>>(emptyList()) }

    LaunchedEffect(Unit) {
        releases = runCatching { container.appUpdateManager.checkForUpdates(force = true) }
            .getOrElse { emptyList() }
        loading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("更新日志") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (releases.isEmpty()) {
                Text(
                    "暂无版本记录，可稍后重试或到 GitHub 查看最新发布。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            releases.forEach { release ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row {
                            Text(
                                text = release.version.ifEmpty { "未命名" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (release.normalizedVersion == AppConstants.APP_VERSION) {
                                Text(
                                    "  当前版本",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (release.prerelease) {
                                Text(
                                    "  预发布",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                        release.publishedAt.takeIf { it.isNotEmpty() }?.let {
                            Text(
                                text = it.take(10),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = release.body.take(1200).ifEmpty { "无说明" },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

/** 帮助页 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("帮助") },
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
        ) {
            val faqs = listOf(
                "如何播放电台？" to "在主页/探索/搜索或收藏中选择电台，点击即可播放；播放页支持上下台切换与音量调节。",
                "后台播放会被打断？" to "请在设置中允许「忽略电池优化」，并保持网络连接；部分国产 ROM 需将应用加入后台白名单。",
                "搜索不到电台？" to "搜索优先在本地缓存进行。若为空，请先在「电台数据更新」中执行一次全量更新。",
                "播放失败提示？" to "可能为电台地址失效、地域限制或网络异常。可点击重试或更换其他电台。",
                "睡眠定时是什么？" to "播放页的时钟图标可设置定时关闭，结束前音量会逐渐减弱后自动停止。",
                "如何导入本地电台？" to "在「本地电台」页点击导入，选择 m3u/m3u8/json 播放列表文件即可。",
            )
            faqs.forEach { (q, a) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(q, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(a, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(
                "数据来源：radio-browser.info 社区电台目录\n" +
                    "开源地址：github.com/${AppConstants.GITHUB_OWNER}/${AppConstants.GITHUB_REPO}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** 播放历史页：查看最近播放的电台（设置页入口） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val history by container.historyStore.history.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("播放历史") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { scope.launch { container.historyStore.clear() } }) {
                            Text("清空")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            com.huanglongmao.onlinefmradio.ui.components.StationListBody(
                stations = history,
                emptyText = "暂无播放记录，去听一首电台吧",
            )
        }
    }
}
