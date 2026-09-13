package com.huanglongmao.onlinefmradio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huanglongmao.onlinefmradio.core.di.LocalAppContainer
import com.huanglongmao.onlinefmradio.core.util.TranslationUtils
import com.huanglongmao.onlinefmradio.core.util.regionOfCountryCode
import com.huanglongmao.onlinefmradio.data.model.Country
import com.huanglongmao.onlinefmradio.data.model.Language
import com.huanglongmao.onlinefmradio.data.model.NameCountDto
import com.huanglongmao.onlinefmradio.data.model.Station
import com.huanglongmao.onlinefmradio.data.model.Tag
import com.huanglongmao.onlinefmradio.ui.components.FilterPickerSheet

/**
 * 维度列表三兄弟（对应 Flutter 版 country_list_page / language_list_page / tag_list_page）。
 * 数据来自本地缓存统计，支持关键词过滤。
 * 国家/语言页额外支持 地区 / 分类 / 标签 筛选（基于本地电台数据聚合）。
 */

// 地区（大洲）映射已下沉至 core/util/RegionUtils.kt，此处直接引用 regionOfCountryCode

// ===== 维度元数据（用于地区/分类/标签筛选）=====

/** 维度条目（国家/语言）聚合出的筛选属性 */
private class DimMeta(val regions: MutableSet<String> = HashSet()) {
    val tags = HashSet<String>()
}

/** 从电台列表按 keySelector 分组构建筛选元数据 */
private fun buildDimMetas(
    stations: List<Station>,
    keySelector: (Station) -> String?,
    regionSelector: (Station) -> String,
): Map<String, DimMeta> {
    val metas = HashMap<String, DimMeta>()
    for (s in stations) {
        val key = keySelector(s) ?: continue
        if (key.isEmpty()) continue
        val meta = metas.getOrPut(key) { DimMeta() }
        meta.regions.add(regionSelector(s))
        s.description.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach {
            meta.tags.add(it)
        }
    }
    return metas
}

/** 筛选目标（地区/标签） */
private const val SHEET_NONE = 0
private const val SHEET_REGION = 1
private const val SHEET_TAG = 2

/** 筛选维度是否全部未启用 */
private fun noFilter(region: String?, tag: String?) = region == null && tag == null

/** 维度筛选条：地区（可选）/ 标签 筛选 Chip + 底部选择弹层 */
@Composable
private fun DimensionFilterBar(
    showRegion: Boolean,
    regions: List<NameCountDto>,
    tags: List<NameCountDto>,
    regionSel: String?,
    tagSel: String?,
    onRegion: (String?) -> Unit,
    onTag: (String?) -> Unit,
) {
    var sheet by remember { mutableIntStateOf(SHEET_NONE) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showRegion) {
            FilterChip(
                selected = regionSel != null,
                onClick = { sheet = SHEET_REGION },
                label = { Text(regionSel?.let { "地区·$it" } ?: "地区") },
                leadingIcon = {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.width(18.dp))
                },
            )
        }
        FilterChip(
            selected = tagSel != null,
            onClick = { sheet = SHEET_TAG },
            label = { Text(tagSel?.let { "标签·$it" } ?: "标签") },
            leadingIcon = {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.width(18.dp))
            },
        )
    }
    when (sheet) {
        SHEET_REGION -> FilterPickerSheet(
            title = "按地区筛选", items = regions, selected = regionSel,
            onSelected = { onRegion(it); sheet = SHEET_NONE },
            onDismiss = { sheet = SHEET_NONE },
        )
        SHEET_TAG -> FilterPickerSheet(
            title = "按标签筛选", items = tags, selected = tagSel,
            onSelected = { onTag(it); sheet = SHEET_NONE },
            onDismiss = { sheet = SHEET_NONE },
        )
    }
}

/** 筛选条件是否命中（meta 为空表示该条目无电台数据，启用筛选时隐藏） */
private fun metaPasses(
    meta: DimMeta?,
    regionSel: String?,
    tagSel: String?,
): Boolean {
    if (noFilter(regionSel, tagSel)) return true
    if (meta == null) return false
    if (regionSel != null && regionSel !in meta.regions) return false
    if (tagSel != null && meta.tags.none { it.equals(tagSel, ignoreCase = true) }) return false
    return true
}

/** 筛选后的地区选项（仅展示数据中出现的地区，附条目数） */
private fun regionOptions(metas: Map<String, DimMeta>): List<NameCountDto> =
    metas.values
        .flatMap { it.regions }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .map { NameCountDto(name = it.key, stationCount = it.value) }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DimensionListScaffold(
    title: String,
    loading: Boolean,
    names: List<Triple<String, String?, Int>>, // (名称, 附加码, 数量)
    labelOf: (String) -> String = { it },
    onBack: () -> Unit,
    onOpen: (name: String, extra: String) -> Unit,
    filterBar: (@Composable () -> Unit)? = null,
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(names, filter) {
        if (filter.isBlank()) names
        else names.filter { it.first.contains(filter, ignoreCase = true) }
    }
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder = { Text("筛选") },
                singleLine = true,
            )
            filterBar?.invoke()
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

/** 国家列表页（支持 标签 筛选） */
@Composable
fun CountryListScreen(onBack: () -> Unit, onOpen: (name: String, code: String) -> Unit) {
    val repo = LocalAppContainer.current.stationRepository
    var loading by remember { mutableStateOf(true) }
    var countries by remember { mutableStateOf<List<Country>>(emptyList()) }
    var metas by remember { mutableStateOf(emptyMap<String, DimMeta>()) }
    var tagOptions by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var tagSel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val stations = repo.loadStations()
        countries = repo.loadCountries()
        metas = buildDimMetas(
            stations,
            keySelector = { it.country },
            regionSelector = { regionOfCountryCode(it.countryCode) },
        )
        tagOptions = repo.loadTags()
        loading = false
    }

    val visible = remember(countries, metas, tagSel) {
        if (tagSel == null) countries
        else countries.filter { metaPasses(metas[it.name], null, tagSel) }
    }
    DimensionListScaffold(
        title = "国家",
        loading = loading,
        names = visible.map { Triple(it.name, it.countryCode, it.stationCount) },
        onBack = onBack,
        onOpen = onOpen,
        filterBar = {
            DimensionFilterBar(
                showRegion = false,
                regions = emptyList(),
                tags = tagOptions.map { NameCountDto(name = it.name, stationCount = it.stationCount) },
                regionSel = null,
                tagSel = tagSel,
                onRegion = { },
                onTag = { tagSel = it },
            )
        },
    )
}

/** 语言列表页（支持 地区/标签 筛选；地区由该语言电台的所属国家推导） */
@Composable
fun LanguageListScreen(onBack: () -> Unit, onOpen: (name: String) -> Unit) {
    val repo = LocalAppContainer.current.stationRepository
    var loading by remember { mutableStateOf(true) }
    var languages by remember { mutableStateOf<List<Language>>(emptyList()) }
    var metas by remember { mutableStateOf(emptyMap<String, DimMeta>()) }
    var tagOptions by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var regionSel by remember { mutableStateOf<String?>(null) }
    var tagSel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val stations = repo.loadStations()
        languages = repo.loadLanguages()
        metas = buildDimMetas(
            stations,
            keySelector = { it.language },
            regionSelector = { regionOfCountryCode(it.countryCode) },
        )
        tagOptions = repo.loadTags()
        loading = false
    }

    val visible = remember(languages, metas, regionSel, tagSel) {
        if (noFilter(regionSel, tagSel)) languages
        else languages.filter { metaPasses(metas[it.name], regionSel, tagSel) }
    }
    DimensionListScaffold(
        title = "语言",
        loading = loading,
        names = visible.map { Triple(it.name, null as String?, it.stationCount) },
        labelOf = { TranslationUtils.getLanguageDisplayName(it) },
        onBack = onBack,
        onOpen = { name, _ -> onOpen(name) },
        filterBar = {
            DimensionFilterBar(
                showRegion = true,
                regions = remember(metas) { regionOptions(metas) },
                tags = tagOptions.map { NameCountDto(name = it.name, stationCount = it.stationCount) },
                regionSel = regionSel,
                tagSel = tagSel,
                onRegion = { regionSel = it },
                onTag = { tagSel = it },
            )
        },
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
    load: suspend () -> List<Station>,
    onBack: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var stations by remember { mutableStateOf<List<Station>>(emptyList()) }
    var visibleCount by remember { mutableStateOf(30) }

    LaunchedEffect(Unit) {
        stations = load()
        loading = false
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
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
