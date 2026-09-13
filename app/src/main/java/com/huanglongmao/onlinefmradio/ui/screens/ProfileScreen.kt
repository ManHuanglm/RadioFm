package com.huanglongmao.onlinefmradio.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.data.model.ReleaseNote
import com.huanglongmao.onlinefmradio.ui.Routes
import com.huanglongmao.onlinefmradio.ui.components.StationLogo
import com.huanglongmao.onlinefmradio.ui.components.playAction
import kotlinx.coroutines.launch

/**
 * 我的页（对应 Flutter 版 profile_page.dart）：
 * 功能入口菜单 + 应用更新检查。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onOpenDrawer: () -> Unit, onNavigate: (String) -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val play = playAction()
    val history by container.historyStore.history.collectAsStateWithLifecycle()
    var showAbout by remember { mutableStateOf(false) }

    // 静默检查更新（24h 间隔）
    var newRelease by remember { mutableStateOf<ReleaseNote?>(null) }
    LaunchedEffect(Unit) {
        val releases = runCatching { container.appUpdateManager.checkForUpdates(force = false) }
            .getOrElse { emptyList() }
        val latest = container.appUpdateManager.latestStable(releases)
        if (latest != null && container.appUpdateManager.isNewer(latest) &&
            !container.appUpdateManager.isSkipped(latest.normalizedVersion)
        ) {
            newRelease = latest
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("我的") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "菜单")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // 用户信息头部
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = AppConstants.APP_NAME,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "版本 ${AppConstants.APP_VERSION}（${AppConstants.APP_BUILD_NUMBER}）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 播放记录
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("播放记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    TextButton(onClick = { scope.launch { container.historyStore.clear() } }) {
                        Text("清空")
                    }
                }
            }
            if (history.isEmpty()) {
                Text(
                    text = "暂无播放记录，去听一首电台吧",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                ) {
                    items(history, key = { it.id }) { station ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(76.dp)
                                .clickable { play(station) },
                        ) {
                            StationLogo(station = station, size = 64.dp, cornerRadius = 16.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()

            // 功能菜单（卡片化）
            val items = listOf(
                ProfileMenuItem("设置", Icons.Filled.Settings) { onNavigate(Routes.SETTINGS) },
                ProfileMenuItem("搜索电台", Icons.Filled.Explore) { onNavigate(Routes.SEARCH) },
                ProfileMenuItem("电台数据更新", Icons.Filled.CloudDownload) { onNavigate(Routes.STATION_UPDATE) },
                ProfileMenuItem("缓存电台", Icons.Filled.Save) { onNavigate(Routes.CACHED_STATIONS) },
                ProfileMenuItem("本地电台", Icons.Filled.Description) { onNavigate(Routes.LOCAL_STATIONS) },
                ProfileMenuItem("随机电台", Icons.Filled.Shuffle) { onNavigate(Routes.RANDOM_STATION) },
                ProfileMenuItem("更新日志", Icons.Filled.Description) { onNavigate(Routes.CHANGELOG) },
                ProfileMenuItem("帮助", Icons.Filled.Help) { onNavigate(Routes.HELP) },
                ProfileMenuItem("检查更新", Icons.Filled.SystemUpdate) {
                    scope.launch {
                        val releases = runCatching {
                            container.appUpdateManager.checkForUpdates(force = true)
                        }.getOrElse { emptyList() }
                        val latest = container.appUpdateManager.latestStable(releases)
                        newRelease = if (latest != null && container.appUpdateManager.isNewer(latest)) {
                            latest
                        } else {
                            // 无更新：弹关于
                            showAbout = true
                            null
                        }
                    }
                },
                ProfileMenuItem("关于", Icons.Filled.Info) { showAbout = true },
            )
            // 菜单卡片容器
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(16.dp),
                    ),
            ) {
                items.forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.label) },
                        leadingContent = {
                            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.clickable(onClick = item.action),
                    )
                }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(AppConstants.APP_NAME) },
            text = {
                Text(
                    "全球 5 万+ 在线电台聚合播放器\n" +
                        "数据来源：radio-browser.info\n" +
                        "版本：${AppConstants.APP_VERSION}\n" +
                        "作者：Huanglongmao",
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("知道了") }
            },
        )
    }

    // 应用更新对话框
    newRelease?.let { release ->
        AlertDialog(
            onDismissRequest = { newRelease = null },
            title = { Text("发现新版本 ${release.normalizedVersion}") },
            text = { Text(release.body.take(400).ifEmpty { "修复若干问题，优化使用体验。" }) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        scope.launch { container.appUpdateManager.skipVersion(release.normalizedVersion) }
                        newRelease = null
                    }) { Text("跳过此版本") }
                    TextButton(onClick = {
                        val url = release.apkDownloadUrl ?: release.htmlUrl
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                        newRelease = null
                    }) { Text("去下载") }
                }
            },
            dismissButton = {},
        )
    }
}

private data class ProfileMenuItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val action: () -> Unit,
)
