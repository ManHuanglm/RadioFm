package com.huanglongmao.onlinefmradio.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.data.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 开发者功能页（从设置页进入）：
 * - 应用日志查看
 * - 本地缓存电台导入（json / m3u / m3u8）
 * - 本地缓存电台导出：按国家 / 语言 / 类型 / 标签筛选，支持 json / m3u / m3u8 格式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(onBack: () -> Unit, onNavigateLogs: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }

    // 导出内容暂存（launcher 回调中写入所选 Uri）
    var exportPendingContent by remember { mutableStateOf<String?>(null) }
    var exportPendingFormat by remember { mutableStateOf("json") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val content = exportPendingContent
        if (uri == null || content == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = container.importExportManager.exportToUri(context, uri, content)
            Toast.makeText(
                context,
                if (ok) "导出成功（${exportPendingFormat.uppercase()}）" else "导出失败",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val stations = container.importExportManager.importFromUri(context, uri)
            if (stations.isEmpty()) {
                Toast.makeText(context, "未解析到有效电台数据", Toast.LENGTH_LONG).show()
            } else {
                container.stationCache.appendStations(stations)
                Toast.makeText(context, "导入完成：新增 ${stations.size} 条", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("开发者功能") },
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
            // 应用日志
            Text("诊断", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onNavigateLogs, modifier = Modifier.fillMaxWidth()) {
                Text("应用日志")
            }

            // 本地缓存电台导入导出
            Text("本地电台缓存", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showExportDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("导出电台缓存…")
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导入电台数据…") }
            Text(
                "导入支持 JSON / M3U / M3U8，数据将按 ID 去重后合并进本地缓存。" +
                    "导出前可按国家、语言、类型、标签筛选。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showExportDialog) {
        ExportFilterDialog(
            onDismiss = { showExportDialog = false },
            onExport = { stations, format ->
                showExportDialog = false
                scope.launch {
                    val content = withContext(Dispatchers.IO) {
                        container.importExportManager.generate(stations, format)
                    }
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    exportPendingContent = content
                    exportPendingFormat = format
                    exportLauncher.launch("stations_export_$stamp.$format")
                }
            },
        )
    }
}

private data class FilterField(val key: String, val label: String)

/**
 * 导出筛选对话框：国家 / 语言 / 类型 / 标签单选（含"全部"），格式与数量上限。
 */
@Composable
private fun ExportFilterDialog(
    onDismiss: () -> Unit,
    onExport: (stations: List<Station>, format: String) -> Unit,
) {
    val container = LocalAppContainer.current
    var all by remember { mutableStateOf<List<Station>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    var country by remember { mutableStateOf<String?>(null) }
    var language by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf<String?>(null) }
    var tag by remember { mutableStateOf<String?>(null) }
    var format by remember { mutableStateOf("json") }
    var limit by remember { mutableStateOf<Int?>(null) }

    var pickingField by remember { mutableStateOf<FilterField?>(null) }

    LaunchedEffect(Unit) {
        all = container.stationCache.getAll()
        loading = false
    }

    val filtered = all.filter { s ->
        (country == null || s.country == country) &&
            (language == null || s.language.contains(language.orEmpty(), ignoreCase = true)) &&
            (category == null || s.category == category) &&
            (tag == null || s.description.contains(tag.orEmpty(), ignoreCase = true))
    }
    val limitValue = limit
    val limited = if (limitValue == null) filtered else filtered.take(limitValue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出电台缓存") },
        text = {
            if (loading) {
                Text("正在读取本地缓存…")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 筛选字段
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = country != null,
                            onClick = { pickingField = FilterField("country", "国家") },
                            label = { Text(country?.let { "国家：$it" } ?: "国家：全部") },
                        )
                        FilterChip(
                            selected = language != null,
                            onClick = { pickingField = FilterField("language", "语言") },
                            label = { Text(language?.let { "语言：$it" } ?: "语言：全部") },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = category != null,
                            onClick = { pickingField = FilterField("category", "类型") },
                            label = { Text(category?.let { "类型：$it" } ?: "类型：全部") },
                        )
                        FilterChip(
                            selected = tag != null,
                            onClick = { pickingField = FilterField("tag", "标签") },
                            label = { Text(tag?.let { "标签：$it" } ?: "标签：全部") },
                        )
                    }

                    // 格式
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("json" to "JSON", "m3u" to "M3U", "m3u8" to "M3U8").forEach { (f, label) ->
                            FilterChip(
                                selected = format == f,
                                onClick = { format = f },
                                label = { Text(label) },
                            )
                        }
                    }

                    // 数量上限
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "全部", 1000 to "前1000", 5000 to "前5000").forEach { (v, label) ->
                            FilterChip(
                                selected = limit == v,
                                onClick = { limit = v },
                                label = { Text(label) },
                            )
                        }
                    }

                    Text(
                        "共 ${all.size} 条缓存，当前筛选命中 ${limited.size} 条",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(limited, format) },
                enabled = !loading && limited.isNotEmpty(),
            ) { Text("导出") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    // 字段选择子对话框（单选列表 + 全部）
    pickingField?.let { field ->
        val options: List<String> = when (field.key) {
            "country" -> all.map { it.country }.filter { it.isNotEmpty() }.distinct().sorted()
            "language" -> all.map { it.language }.filter { it.isNotEmpty() }.distinct().sorted()
            "category" -> all.map { it.category }.filter { it.isNotEmpty() }.distinct().sorted()
            else -> all.flatMap { it.description.split(',') }.map { it.trim() }
                .filter { it.isNotEmpty() }.distinct().sorted()
        }
        val current: String? = when (field.key) {
            "country" -> country
            "language" -> language
            "category" -> category
            else -> tag
        }
        AlertDialog(
            onDismissRequest = { pickingField = null },
            title = { Text("选择${field.label}") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (field.key) {
                                        "country" -> country = null
                                        "language" -> language = null
                                        "category" -> category = null
                                        else -> tag = null
                                    }
                                    pickingField = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = current == null, onClick = null)
                            Text("  全部")
                        }
                    }
                    items(options) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (field.key) {
                                        "country" -> country = option
                                        "language" -> language = option
                                        "category" -> category = option
                                        else -> tag = option
                                    }
                                    pickingField = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = current == option, onClick = null)
                            Text("  $option")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickingField = null }) { Text("完成") } },
        )
    }
}
