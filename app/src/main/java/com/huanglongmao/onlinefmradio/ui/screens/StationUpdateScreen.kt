package com.huanglongmao.onlinefmradio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer

/**
 * 电台数据更新页（对应 Flutter 版 station_update_service 的 UI 使用方）：
 * 缓存/远程对比 + 进度条 + 暂停/继续/重新获取 + 断点续传提示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationUpdateScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val manager = container.stationUpdateManager
    val view = LocalView.current

    val isUpdating by manager.isUpdating.collectAsStateWithLifecycle()
    val isPaused by manager.isPaused.collectAsStateWithLifecycle()
    val fetched by manager.fetchedCount.collectAsStateWithLifecycle()
    val total by manager.totalCount.collectAsStateWithLifecycle()
    val complete by manager.updateComplete.collectAsStateWithLifecycle()
    val hasResume by manager.hasResumeData.collectAsStateWithLifecycle()
    val errorMsg by manager.errorMessage.collectAsStateWithLifecycle()
    val cachedCount by manager.cachedCount.collectAsStateWithLifecycle()
    val remoteStats by manager.remoteStats.collectAsStateWithLifecycle()

    var confirmRestart by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }

    // 更新期间屏幕常亮（对齐原版 WakelockPlus）
    DisposableEffect(isUpdating) {
        view.keepScreenOn = isUpdating
        onDispose { view.keepScreenOn = false }
    }

    // 进入页面刷新统计
    LaunchedEffect(Unit) { manager.refreshStats() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("电台数据更新") },
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 统计信息
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCard("本地缓存", cachedCount.toString())
                StatCard("远程总数", (remoteStats?.stations ?: 0).toString())
                StatCard("国家/语言", "${remoteStats?.countries ?: 0}/${remoteStats?.languages ?: 0}")
            }

            if (hasResume && !isUpdating) {
                Text(
                    text = "检测到未完成的更新任务（已获取 $fetched 条），可继续更新。",
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            if (isUpdating) {
                val progress = if (total > 0) fetched.toFloat() / total else 0f
                Text("正在更新…（$fetched / $total）", fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )
                Text("更新期间请保持网络畅通，可随时暂停。", style = MaterialTheme.typography.bodySmall)
            } else if (complete && errorMsg == null) {
                Text(
                    text = "更新完成，本地已缓存 $cachedCount 条电台数据。",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            // 操作按钮
            if (!isUpdating) {
                Button(
                    onClick = {
                        started = true
                        manager.startRefresh()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (hasResume) "继续更新" else if (cachedCount > 0) "检查并更新" else "开始更新")
                }
                if (cachedCount > 0) {
                    OutlinedButton(
                        onClick = { confirmRestart = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("清空重新获取") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!isPaused) {
                        OutlinedButton(onClick = { manager.pause() }, modifier = Modifier.weight(1f)) {
                            Text("暂停")
                        }
                    } else {
                        Button(onClick = { manager.resumeUpdate() }, modifier = Modifier.weight(1f)) {
                            Text("继续")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            manager.stop()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("停止") }
                }
            }

            if (!started && !complete && !isUpdating && cachedCount == 0) {
                Text(
                    "首次使用建议先执行全量更新，" +
                        "完成后即可离线浏览全部电台（约 1 万条）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("重新获取") },
            text = { Text("将清空本地全部缓存并重新下载，确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestart = false
                    manager.startRestart()
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
