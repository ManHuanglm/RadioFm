package com.huanglongmao.onlinefmradio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.data.model.Station
import com.huanglongmao.onlinefmradio.ui.components.StationListBody
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * 缓存电台页（对应 Flutter 版 cached_stations_page.dart）：
 * 全量缓存列表 + 关键词过滤 + 一键清空。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CachedStationsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var stations by remember { mutableStateOf<List<Station>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        stations = container.stationRepository.loadStations()
        loading = false
    }

    val filtered = remember(stations, filter) {
        if (filter.isBlank()) stations
        else stations.filter {
            it.name.contains(filter, true) || it.country.contains(filter, true) ||
                it.category.contains(filter, true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("缓存电台（${stations.size}）") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "清空缓存")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder = { Text("过滤名称 / 国家 / 分类") },
                singleLine = true,
            )
            if (loading) {
                CircularProgressIndicator(Modifier.padding(32.dp))
            } else {
                StationListBody(stations = filtered.take(300), emptyText = "缓存为空，请先执行数据更新")
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空缓存") },
            text = { Text("将删除全部 ${stations.size} 条缓存电台数据，确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    loading = true
                    scope.launch {
                        container.stationRepository.clearCache()
                        stations = emptyList()
                        loading = false
                    }
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            },
        )
    }
}

/**
 * 本地电台页（对应 Flutter 版 local_stations_page.dart）：
 * SAF 导入 m3u/m3u8/json + 列表管理。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalStationsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val stations by container.localStationStore.stations.collectAsListState()
    var filter by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val imported = container.importExportManager.importFromUri(context, uri)
                if (imported.isEmpty()) {
                    snackbar.showSnackbar("未解析到有效电台，请使用 m3u/m3u8/json 格式")
                } else {
                    val added = container.localStationStore.importStations(imported)
                    snackbar.showSnackbar("导入完成：新增 $added 个电台")
                }
            }
        }
    }

    val filtered = remember(stations, filter) {
        if (filter.isBlank()) stations
        else stations.filter { it.name.contains(filter, true) || it.country.contains(filter, true) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("本地电台（${stations.size}）") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Filled.FileUpload, contentDescription = "导入")
                    }
                    IconButton(onClick = {
                        scope.launch { container.localStationStore.clearAll() }
                    }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "清空")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder = { Text("过滤") },
                singleLine = true,
            )
            StationListBody(
                stations = filtered.take(300),
                emptyText = "还没有本地电台，点击右上角导入 m3u / json 文件",
            )
        }
    }
}

/** StateFlow<List<T>> 的 collectAsState 简写 */
@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<List<T>>.collectAsListState(): androidx.compose.runtime.State<List<T>> =
    collectAsStateWithLifecycle()

/**
 * 随机电台页（对应 Flutter 版 random_station_page.dart）：
 * 抽一个随机电台直接播放。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomStationScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Station?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("随机电台") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            result?.let { station ->
                com.huanglongmao.onlinefmradio.ui.components.StationCard(
                    station = station,
                    isPlaying = false,
                    isFavorite = false,
                    onClickPlay = { container.playerController.play(station) },
                    onClickFavorite = {
                        scope.launch { container.favoritesStore.toggle(station) }
                    },
                )
                Spacer(Modifier.padding(8.dp))
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.padding(8.dp))
            }
            Button(
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        val station = runCatching {
                            container.radioBrowserApi.random(limit = 1)
                                .firstNotNullOfOrNull { it.toStation() }
                        }.getOrElse { null }
                        result = station
                        if (station == null) error = "获取随机电台失败，请检查网络"
                        loading = false
                    }
                },
                enabled = !loading,
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.width(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("随机来一个")
            }
        }
    }
}
