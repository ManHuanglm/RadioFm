package com.huanglongmao.onlinefmradio.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanglongmao.onlinefmradio.App
import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.core.util.REGION_ORDER
import com.huanglongmao.onlinefmradio.core.util.TranslationUtils
import com.huanglongmao.onlinefmradio.core.util.regionOfCountryCode
import com.huanglongmao.onlinefmradio.data.model.Country
import com.huanglongmao.onlinefmradio.data.model.Language
import com.huanglongmao.onlinefmradio.data.model.NameCountDto
import com.huanglongmao.onlinefmradio.data.model.Station
import com.huanglongmao.onlinefmradio.ui.Routes
import com.huanglongmao.onlinefmradio.ui.components.FilterPickerSheet
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
    val scope = rememberCoroutineScope()

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
    // 推荐 Tab 输入框筛选（名称/国家/语言/标签）
    var recommendQuery by remember { mutableStateOf("") }
    val recommendedFiltered = remember(recommended, recommendQuery) {
        val q = recommendQuery.trim().lowercase()
        if (q.isEmpty()) {
            recommended
        } else {
            recommended.filter { s ->
                s.name.lowercase().contains(q) ||
                    s.country.lowercase().contains(q) ||
                    s.language.lowercase().contains(q) ||
                    s.description.lowercase().contains(q)
            }
        }
    }
    // 输入变化时重置分页
    LaunchedEffect(recommendQuery) {
        visibleCount = AppConstants.PAGE_SIZE
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
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
                tab == 0 -> Column(Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = recommendQuery,
                        onValueChange = { recommendQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        placeholder = { Text("筛选电台：名称/国家/标签") },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (recommendQuery.isNotEmpty()) {
                                IconButton(onClick = { recommendQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "清除")
                                }
                            }
                        },
                    )
                    StationListBody(
                        stations = recommendedFiltered.take(visibleCount),
                        hasMore = visibleCount < recommendedFiltered.size,
                        onReachEnd = { visibleCount += AppConstants.PAGE_SIZE },
                        emptyText = if (recommendQuery.isNotBlank()) "未找到匹配的电台"
                        else if (preferredCountry != null) "该国家暂无电台"
                        else "暂无数据，请在抽屉中先更新电台数据",
                    )
                }
                tab == 1 -> DimensionFilterTab(
                    dimensionLabel = "国家",
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
                    dimensionLabel = "语言",
                    items = languages.map { NameCountDto(name = it.name, stationCount = it.stationCount) },
                    initialSelection = null,
                    onSelectionChanged = { },
                    all = sorted,
                    matchOf = { s, name -> s.language.lowercase().contains(name.lowercase()) },
                    labelOf = { TranslationUtils.getLanguageDisplayName(it.name) },
                    showRegion = true,
                    emptyText = "暂无数据，请在抽屉中先更新电台数据",
                )
            }
        }
    }
}

/**
 * 国家/语言 Tab：级联筛选。
 * 国家 Tab：国家 → 标签；语言 Tab：语言 → 地区 → 标签（showRegion = true）。
 * 每级均为底部弹层选择（LazyColumn 懒加载，低配机流畅）；
 * 选项基于上一级过滤结果动态收窄，上级变更时自动清空下级选择。
 */
@Composable
private fun DimensionFilterTab(
    dimensionLabel: String,
    items: List<NameCountDto>,
    initialSelection: String?,
    onSelectionChanged: (String?) -> Unit,
    all: List<Station>,
    matchOf: (Station, String) -> Boolean,
    labelOf: (NameCountDto) -> String = { it.name },
    showRegion: Boolean = false,
    emptyText: String,
) {
    var selectedDim by remember { mutableStateOf(initialSelection) }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    // 0=无 1=维度 2=地区(仅语言 Tab) 3=标签
    var openSheet by remember { mutableIntStateOf(0) }
    var visibleCount by remember { mutableIntStateOf(AppConstants.PAGE_SIZE) }

    // ===== 级联过滤链 =====
    val afterDim = remember(all, selectedDim) {
        val d = selectedDim
        if (d == null) all else all.filter { matchOf(it, d) }
    }
    val afterRegion = remember(afterDim, selectedRegion, showRegion) {
        val r = selectedRegion
        if (!showRegion || r == null) afterDim else afterDim.filter { regionOfCountryCode(it.countryCode) == r }
    }
    val filtered = remember(afterRegion, selectedTag) {
        val t = selectedTag
        if (t == null) {
            afterRegion
        } else {
            afterRegion.filter { s -> s.description.split(',').any { it.trim().equals(t, ignoreCase = true) } }
        }
    }

    // ===== 各级选项（基于上级结果聚合，带电台数）=====
    val regionOptions = remember(afterDim) {
        afterDim.groupingBy { regionOfCountryCode(it.countryCode) }
            .eachCount()
            .entries
            .sortedBy { REGION_ORDER.indexOf(it.key).let { i -> if (i < 0) REGION_ORDER.size else i } }
            .map { NameCountDto(name = it.key, stationCount = it.value) }
    }
    val tagOptions = remember(afterRegion) {
        afterRegion.asSequence()
            .flatMap { s -> s.description.split(',').asSequence().map { it.trim() } }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(100)
            .map { NameCountDto(name = it.key, stationCount = it.value) }
            .toList()
    }

    // 任一筛选条件变化时重置分页
    LaunchedEffect(selectedDim, selectedRegion, selectedTag) {
        visibleCount = AppConstants.PAGE_SIZE
    }

    Column(Modifier.fillMaxSize()) {
        DimensionFilterChipsRow(
            dimensionLabel = dimensionLabel,
            selectedDim = selectedDim,
            selectedRegion = selectedRegion,
            selectedTag = selectedTag,
            showRegion = showRegion,
            onOpenSheet = { openSheet = it },
        )
        DimensionFilterSheets(
            openSheet = openSheet,
            dimensionLabel = dimensionLabel,
            items = items,
            regionOptions = regionOptions,
            tagOptions = tagOptions,
            selectedDim = selectedDim,
            selectedRegion = selectedRegion,
            selectedTag = selectedTag,
            labelOf = labelOf,
            onSelectDim = { v ->
                selectedDim = v
                // 上级变更，清空下级
                selectedRegion = null
                selectedTag = null
                onSelectionChanged(v)
            },
            onSelectRegion = { v ->
                selectedRegion = v
                selectedTag = null
            },
            onSelectTag = { v -> selectedTag = v },
            onDismiss = { openSheet = 0 },
        )
        StationListBody(
            stations = filtered.take(visibleCount),
            hasMore = visibleCount < filtered.size,
            onReachEnd = { visibleCount += AppConstants.PAGE_SIZE },
            emptyText = emptyText,
        )
    }
}

/** 级联筛选 Chip 行：维度 / 地区 / 标签 */
@Composable
private fun DimensionFilterChipsRow(
    dimensionLabel: String,
    selectedDim: String?,
    selectedRegion: String?,
    selectedTag: String?,
    showRegion: Boolean,
    onOpenSheet: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedDim != null,
            onClick = { onOpenSheet(1) },
            label = { Text(selectedDim?.let { "$dimensionLabel·$it" } ?: dimensionLabel) },
            leadingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.width(18.dp)) },
        )
        if (showRegion) {
            FilterChip(
                selected = selectedRegion != null,
                onClick = { onOpenSheet(2) },
                label = { Text(selectedRegion?.let { "地区·$it" } ?: "地区") },
                leadingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.width(18.dp)) },
            )
        }
        FilterChip(
            selected = selectedTag != null,
            onClick = { onOpenSheet(3) },
            label = { Text(selectedTag?.let { "标签·$it" } ?: "标签") },
            leadingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.width(18.dp)) },
        )
    }
}

/** 级联筛选底部弹层（按 [openSheet] 显示维度/地区/标签三选一） */
@Composable
private fun DimensionFilterSheets(
    openSheet: Int,
    dimensionLabel: String,
    items: List<NameCountDto>,
    regionOptions: List<NameCountDto>,
    tagOptions: List<NameCountDto>,
    selectedDim: String?,
    selectedRegion: String?,
    selectedTag: String?,
    labelOf: (NameCountDto) -> String,
    onSelectDim: (String?) -> Unit,
    onSelectRegion: (String?) -> Unit,
    onSelectTag: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    when (openSheet) {
        1 -> FilterPickerSheet(
            title = "按${dimensionLabel}筛选",
            items = items,
            selected = selectedDim,
            labelOf = labelOf,
            onSelected = {
                onSelectDim(it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        2 -> FilterPickerSheet(
            title = "按地区筛选",
            items = regionOptions,
            selected = selectedRegion,
            onSelected = {
                onSelectRegion(it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        3 -> FilterPickerSheet(
            title = "按标签筛选",
            items = tagOptions,
            selected = selectedTag,
            onSelected = {
                onSelectTag(it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}
