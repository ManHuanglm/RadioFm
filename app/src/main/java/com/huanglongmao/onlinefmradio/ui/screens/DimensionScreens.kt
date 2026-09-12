package com.huanglongmao.onlinefmradio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.core.util.TranslationUtils
import com.huanglongmao.onlinefmradio.data.model.Country
import com.huanglongmao.onlinefmradio.data.model.Language
import com.huanglongmao.onlinefmradio.data.model.Tag

/**
 * 维度列表三兄弟（对应 Flutter 版 country_list_page / language_list_page / tag_list_page）。
 * 数据来自本地缓存统计，支持关键词过滤。
 */

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DimensionListScaffold(
    title: String,
    loading: Boolean,
    names: List<Triple<String, String?, Int>>, // (名称, 附加码, 数量)
    labelOf: (String) -> String = { it },
    onBack: () -> Unit,
    onOpen: (name: String, extra: String) -> Unit,
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(names, filter) {
        if (filter.isBlank()) names
        else names.filter { it.first.contains(filter, ignoreCase = true) }
    }
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder = { Text("筛选") },
                singleLine = true,
            )
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered, key = { it.first }) { (name, extra, count) ->
                        ListItem(
                            headlineContent = { Text(labelOf(name)) },
                            supportingContent = { if (extra?.isNotEmpty() == true) Text(extra) },
                            trailingContent = { Text("$count 台") },
                            modifier = Modifier.clickable { onOpen(name, extra.orEmpty()) },
                        )
                    }
                }
            }
        }
    }
}

/** 国家列表页 */
@Composable
fun CountryListScreen(onBack: () -> Unit, onOpen: (name: String, code: String) -> Unit) {
    val repo = LocalAppContainer.current.stationRepository
    var loading by remember { mutableStateOf(true) }
    var countries by remember { mutableStateOf<List<Country>>(emptyList()) }
    LaunchedEffect(Unit) {
        countries = repo.loadCountries()
        loading = false
    }
    DimensionListScaffold(
        title = "国家",
        loading = loading,
        names = countries.map { Triple(it.name, it.countryCode, it.stationCount) },
        onBack = onBack,
        onOpen = onOpen,
    )
}

/** 语言列表页 */
@Composable
fun LanguageListScreen(onBack: () -> Unit, onOpen: (name: String) -> Unit) {
    val repo = LocalAppContainer.current.stationRepository
    var loading by remember { mutableStateOf(true) }
    var languages by remember { mutableStateOf<List<Language>>(emptyList()) }
    LaunchedEffect(Unit) {
        languages = repo.loadLanguages()
        loading = false
    }
    DimensionListScaffold(
        title = "语言",
        loading = loading,
        names = languages.map { Triple(it.name, null, it.stationCount) },
        labelOf = { TranslationUtils.getLanguageDisplayName(it) },
        onBack = onBack,
        onOpen = { name, _ -> onOpen(name) },
    )
}

/** 标签列表页 */
@Composable
fun TagListScreen(onBack: () -> Unit, onOpen: (tag: String) -> Unit) {
    val repo = LocalAppContainer.current.stationRepository
    var loading by remember { mutableStateOf(true) }
    var tags by remember { mutableStateOf<List<Tag>>(emptyList()) }
    LaunchedEffect(Unit) {
        tags = repo.loadTags()
        loading = false
    }
    DimensionListScaffold(
        title = "标签",
        loading = loading,
        names = tags.map { Triple(it.name, null, it.stationCount) },
        onBack = onBack,
        onOpen = { tag, _ -> onOpen(tag) },
    )
}

/** 维度电台页通用骨架：远程加载 + 本地分页展示 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DimensionStationsScaffold(
    title: String,
    load: suspend () -> List<com.huanglongmao.onlinefmradio.data.model.Station>,
    onBack: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var stations by remember { mutableStateOf<List<com.huanglongmao.onlinefmradio.data.model.Station>>(emptyList()) }
    var visibleCount by remember { mutableStateOf(30) }

    LaunchedEffect(Unit) {
        stations = load()
        loading = false
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                com.huanglongmao.onlinefmradio.ui.components.StationListBody(
                    stations = stations.take(visibleCount),
                    hasMore = visibleCount < stations.size,
                    onReachEnd = { visibleCount += 30 },
                    emptyText = "暂无电台",
                )
            }
        }
    }
}

/** 按国家代码查看电台 */
@Composable
fun CountryStationsScreen(onBack: () -> Unit, name: String, code: String) {
    val repo = LocalAppContainer.current.stationRepository
    DimensionStationsScaffold(
        title = name.ifEmpty { code },
        onBack = onBack,
        load = {
            if (code.isNotEmpty()) repo.loadByCountry(code) else repo.loadByCountryName(name)
        },
    )
}

/** 按语言查看电台 */
@Composable
fun LanguageStationsScreen(onBack: () -> Unit, name: String) {
    val repo = LocalAppContainer.current.stationRepository
    DimensionStationsScaffold(title = name, onBack = onBack, load = { repo.loadByLanguage(name) })
}

/** 按标签查看电台 */
@Composable
fun TagStationsScreen(onBack: () -> Unit, tag: String) {
    val repo = LocalAppContainer.current.stationRepository
    DimensionStationsScaffold(title = tag, onBack = onBack, load = { repo.loadByTag(tag) })
}
