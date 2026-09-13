package com.huanglongmao.onlinefmradio.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.data.model.Station
import com.huanglongmao.onlinefmradio.ui.components.StationListBody
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 搜索页（对应 Flutter 版 search_page.dart）：
 * 300ms 防抖，优先本地缓存搜索（名称/国家/语言/分类/描述），无结果再查远程。
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SearchScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val repo = container.stationRepository

    var keyword by remember { mutableStateOf("") }
    val keywordFlow = remember { MutableStateFlow("") }
    var results by remember { mutableStateOf<List<Station>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        keywordFlow
            .debounce(300)
            .distinctUntilChanged()
            .collect { q ->
                if (q.isBlank()) {
                    results = emptyList()
                } else {
                    searching = true
                    var found = repo.searchCachedStations(q)
                    if (found.isEmpty()) found = repo.searchStationsRemote(q)
                    results = found
                    searching = false
                }
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("搜索电台") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = keyword,
                onValueChange = {
                    keyword = it
                    keywordFlow.value = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("输入电台名称 / 国家 / 语言 / 分类") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        IconButton(onClick = { keyword = ""; keywordFlow.value = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true,
            )
            when {
                searching -> StationListBody(emptyList(), emptyText = "搜索中…")
                keyword.isBlank() -> StationListBody(
                    emptyList(),
                    emptyText = "输入关键词搜索缓存中的电台",
                )
                else -> StationListBody(
                    stations = results,
                    emptyText = "未找到匹配的电台",
                )
            }
        }
    }
}
