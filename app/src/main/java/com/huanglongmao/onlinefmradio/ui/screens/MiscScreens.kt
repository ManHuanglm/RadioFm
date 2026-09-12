package com.huanglongmao.onlinefmradio.ui.screens

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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.data.model.ReleaseNote

/**
 * 外围页面集合：
 * - 录音 / 闹钟占位页（对应 recording_page.dart / alarm_page.dart，标注"开发中"）
 * - 更新日志页（对应 changelog_page.dart，GitHub Releases）
 * - 帮助页（对应 help_page.dart）
 */

/** 占位页通用骨架 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(title: String, emoji: String, message: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
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

/** 录音页（占位） */
@Composable
fun RecordingScreen(onBack: () -> Unit) =
    PlaceholderScreen("录音", "🎙️", "录音功能开发中", onBack)

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
            TopAppBar(
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
            TopAppBar(
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
