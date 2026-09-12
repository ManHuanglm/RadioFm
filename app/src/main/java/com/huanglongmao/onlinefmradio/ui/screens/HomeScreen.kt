package com.huanglongmao.onlinefmradio.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.App
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.core.util.TranslationUtils
import com.huanglongmao.onlinefmradio.data.model.Country
import com.huanglongmao.onlinefmradio.data.model.Language
import com.huanglongmao.onlinefmradio.data.model.NameCountDto
import com.huanglongmao.onlinefmradio.data.model.Station
import com.huanglongmao.onlinefmradio.ui.Routes
import com.huanglongmao.onlinefmradio.ui.components.StationListBody
import kotlinx.coroutines.launch

/** 排序方式 */
private val sortOptions = listOf(
    "votes" to "最多投票",
    "name" to "名称",
    "country" to "国家",
    "lastchecktime" to "最新收录",
)

/**
 * 主页（对应 Flutter 版 home_page.dart）：
 * 3 Tab（推荐/国家/语言）+ 排序菜单 + 国家偏好过滤。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenDrawer: () -> Unit, onNavigate: (String) -> Unit) {
    val container = LocalAppContainer.current
    val repo = container.stationRepository
    val preferredCountry by container.countryPreferenceStore.selectedCountry.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    var all by remember { mutableStateOf<List<Station>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var sortKey by remember { mutableStateOf("votes") }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var visibleCount by remember { mutableIntStateOf(AppConstants.PAGE_SIZE) }
    var countries by remember { mutableStateOf<List<Country>>(emptyList()) }
    var languages by remember { mutableStateOf<List<Language>>(emptyList()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        all = repo.loadStations()
        countries = repo.loadCountries()
        languages = repo.loadLanguages()
        loading = false
    }

    // 排序（本地排序，缓存数据无需反复请求 API）
    val sorted = remember(all, sortKey) {
        when (sortKey) {
            "name" -> all.sortedBy { it.name.lowercase() }
            "country" -> all.sortedBy { it.country }
            else -> all
        }
    }
    // 推荐 Tab 应用国家偏好
    val recommended = remember(sorted, preferredCountry) {
        if (preferredCountry.isNullOrEmpty()) sorted
        else sorted.filter { it.country == preferredCountry }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppConstants.APP_NAME) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "菜单")
                    }
                },
                actions = {
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "排序")
                    }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        sortOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    sortKey = key
                                    visibleCount = AppConstants.PAGE_SIZE
                                    sortMenuOpen = false
                                },
                            )
                        }
                    }
                    IconButton(onClick = { onNavigate(Routes.SEARCH) }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SecondaryTabRow(selectedTabIndex = tab) {
                listOf("推荐", "国家", "语言").forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }
            when {
                loading -> CircularProgressIndicator(Modifier.padding(32.dp))
                tab == 0 -> StationListBody(
                    stations = recommended.take(visibleCount),
                    hasMore = visibleCount < recommended.size,
                    onReachEnd = { visibleCount += AppConstants.PAGE_SIZE },
                    emptyText = if (preferredCountry != null) "该国家暂无电台"
                    else "暂无数据，请在抽屉中先更新电台数据",
                )
                tab == 1 -> DimensionFilterTab(
                    items = countries.map {
                        NameCountDto(name = it.name, iso31661 = it.countryCode, stationCount = it.stationCount)
                    },
                    initialSelection = preferredCountry,
                    onSelectionChanged = { name ->
                        scope.launch { container.countryPreferenceStore.setCountry(name) }
                    },
                    all = sorted,
                    matchOf = { s, name -> s.country == name },
                    emptyText = "暂无数据，请在抽屉中先更新电台数据",
                )
                tab == 2 -> DimensionFilterTab(
                    items = languages.map { NameCountDto(name = it.name, stationCount = it.stationCount) },
                    initialSelection = null,
                    onSelectionChanged = { },
                    all = sorted,
                    matchOf = { s, name -> s.language.lowercase().contains(name.lowercase()) },
                    labelOf = { TranslationUtils.getLanguageDisplayName(it.name) },
                    emptyText = "暂无数据，请在抽屉中先更新电台数据",
                )
            }
        }
    }
}

/** 国家/语言 Tab：下拉选择维度 → 本地过滤电台列表 */
@Composable
private fun DimensionFilterTab(
    items: List<NameCountDto>,
    initialSelection: String?,
    onSelectionChanged: (String?) -> Unit,
    all: List<Station>,
    matchOf: (Station, String) -> Boolean,
    labelOf: (NameCountDto) -> String = { it.name },
    emptyText: String,
) {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(initialSelection) }
    var visibleCount by remember { mutableIntStateOf(AppConstants.PAGE_SIZE) }

    Column(Modifier.fillMaxSize()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(selected?.let { "已选：$selected" } ?: "选择筛选条件")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("全部") }, onClick = {
                selected = null
                onSelectionChanged(null)
                visibleCount = AppConstants.PAGE_SIZE
                expanded = false
            })
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(labelOf(item)) },
                    onClick = {
                        selected = item.name
                        onSelectionChanged(item.name)
                        visibleCount = AppConstants.PAGE_SIZE
                        expanded = false
                    },
                )
            }
        }
        val sel = selected
        val filtered = if (sel == null) all else all.filter { matchOf(it, sel) }
        StationListBody(
            stations = filtered.take(visibleCount),
            hasMore = visibleCount < filtered.size,
            onReachEnd = { visibleCount += AppConstants.PAGE_SIZE },
            emptyText = emptyText,
        )
    }
}
