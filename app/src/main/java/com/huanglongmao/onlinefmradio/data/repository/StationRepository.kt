package com.huanglongmao.onlinefmradio.data.repository

import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.data.cache.AssetFallback
import com.huanglongmao.onlinefmradio.data.cache.StationFileCache
import com.huanglongmao.onlinefmradio.data.model.Country
import com.huanglongmao.onlinefmradio.data.model.Language
import com.huanglongmao.onlinefmradio.data.model.RadioStats
import com.huanglongmao.onlinefmradio.data.model.Station
import com.huanglongmao.onlinefmradio.data.model.StationDto
import com.huanglongmao.onlinefmradio.data.model.Tag
import com.huanglongmao.onlinefmradio.data.remote.RadioBrowserApi
import kotlinx.coroutines.delay

/**
 * 电台数据仓库（合并 Flutter 版 station_repository.dart + local_station_datasource.dart）。
 *
 * 加载策略：本地缓存 → API → assets 兜底。
 */
class StationRepository(
    private val api: RadioBrowserApi,
    private val cache: StationFileCache,
    private val assetFallback: AssetFallback,
) {

    private var cachedStats: RadioStats? = null

    /** 过滤无效电台（无流地址） */
    private fun List<StationDto>.toValidStations(): List<Station> =
        mapNotNull { it.toStation() }.filter { it.streamUrl.isNotEmpty() }

    // ===== 基础加载 =====

    /**
     * 加载电台列表。
     * [forceRefresh] 为 false 且存在缓存时直接返回缓存；
     * 否则从 API 加载并缓存，API 失败时回退到 assets 兜底。
     */
    suspend fun loadStations(forceRefresh: Boolean = false): List<Station> {
        if (!forceRefresh && cache.hasCache()) {
            val cached = cache.getAll()
            if (cached.isNotEmpty()) return cached
        }
        return loadFromApiAndCache()
    }

    /** 分页加载更多电台（按 votes 倒序），并追加进缓存 */
    suspend fun loadMoreStations(offset: Int): List<Station> = try {
        val stations = api.stations(
            limit = AppConstants.PAGE_SIZE,
            offset = offset,
            order = "votes",
            reverse = "true",
        ).toValidStations()
        cache.appendStations(stations)
        stations
    } catch (_: Exception) {
        emptyList()
    }

    /** 从 API 获取投票最多的电台并缓存，失败回退 assets */
    private suspend fun loadFromApiAndCache(): List<Station> {
        val stations: List<Station> = try {
            api.stations(limit = AppConstants.PAGE_SIZE, order = "votes", reverse = "true")
                .toValidStations()
        } catch (_: Exception) {
            return assetFallback.load()
        }
        runCatching { cache.replaceAll(stations) }
        return stations
    }

    // ===== 全量更新（断点续传）=====

    /**
     * 全量获取电台数据并缓存。
     *
     * @param onProgress 进度回调 (已获取, 总量)
     * @param resumeOffset 断点续传起始偏移，0 表示从头开始
     * @param resumeFetched 断点续传已获取数量
     * @param onBatchSaved 每批保存后回调 (下一 offset, 已获取, 总量)
     * @param isPaused 返回 true 时暂停等待
     * @param shouldStop 返回 true 时取消
     */
    suspend fun fetchAllAndCache(
        resumeOffset: Int = 0,
        resumeFetched: Int = 0,
        onProgress: suspend (fetched: Int, total: Int) -> Unit = { _, _ -> },
        onBatchSaved: suspend (offset: Int, fetched: Int, total: Int) -> Unit = { _, _, _ -> },
        isPaused: () -> Boolean = { false },
        shouldStop: () -> Boolean = { false },
    ): Int {
        val totalStations = getMaxStations()
        var fetchedCount = resumeFetched

        // 本地已缓存 ID 集合，用于差异对比
        val existingIds = cache.getCachedIds().toMutableSet()

        val offsets = mutableListOf<Int>()
        var i = resumeOffset
        while (i < totalStations) {
            offsets.add(i)
            i += AppConstants.BATCH_SIZE
        }

        for (currentOffset in offsets) {
            if (shouldStop()) return fetchedCount

            // 暂停等待
            while (isPaused()) {
                if (shouldStop()) return fetchedCount
                delay(200)
            }

            try {
                val jsonData = api.stations(
                    limit = AppConstants.BATCH_SIZE,
                    offset = currentOffset,
                    order = "votes",
                    reverse = "true",
                )
                if (jsonData.isEmpty()) break

                // ID 级初筛，避免重复构造对象
                val newStations = ArrayList<Station>(jsonData.size)
                for (dto in jsonData) {
                    val uuid = dto.stationUuid ?: continue
                    if (uuid.isEmpty() || existingIds.contains(uuid)) continue
                    val station = dto.toStation() ?: continue
                    newStations.add(station)
                    existingIds.add(station.id)
                }

                if (newStations.isNotEmpty()) {
                    cache.appendStations(newStations)
                    fetchedCount += newStations.size
                    onProgress(fetchedCount, totalStations)
                }

                onBatchSaved(currentOffset + AppConstants.BATCH_SIZE, fetchedCount, totalStations)

                // 批次间让渡，避免与音频播放竞争
                delay(AppConstants.BATCH_DELAY_MS)

                if (jsonData.size < AppConstants.BATCH_SIZE) break
            } catch (_: Exception) {
                // 单批失败继续下一批
            }
        }
        return fetchedCount
    }

    /** 同步远程数据：从本地末端开始增量比对，只写入新增数据 */
    suspend fun syncRemoteStations(
        onProgress: suspend (compared: Int, total: Int) -> Unit = { _, _ -> },
        shouldStop: () -> Boolean = { false },
    ): Int {
        val totalStations = getMaxStations()
        val existingIds = cache.getCachedIds().toMutableSet()
        val startOffset = existingIds.size
        var newCount = 0
        var comparedCount = startOffset

        var i = startOffset
        while (i < totalStations) {
            if (shouldStop()) break
            try {
                val jsonData = api.stations(
                    limit = AppConstants.BATCH_SIZE,
                    offset = i,
                    order = "votes",
                    reverse = "true",
                )
                if (jsonData.isEmpty()) break

                val newStations = ArrayList<Station>(jsonData.size)
                for (dto in jsonData) {
                    val uuid = dto.stationUuid ?: continue
                    if (uuid.isEmpty() || existingIds.contains(uuid)) continue
                    val station = dto.toStation() ?: continue
                    newStations.add(station)
                    existingIds.add(station.id)
                }

                if (newStations.isNotEmpty()) {
                    cache.appendStations(newStations)
                    newCount += newStations.size
                }

                comparedCount += jsonData.size
                onProgress(comparedCount, totalStations)
                delay(AppConstants.BATCH_DELAY_MS)

                if (jsonData.size < AppConstants.BATCH_SIZE) break
            } catch (_: Exception) {
            }
            i += AppConstants.BATCH_SIZE
        }
        return newCount
    }

    /** 获取电台总数：优先 stats API，失败用默认值 */
    private suspend fun getMaxStations(): Int = try {
        val stats = loadStats()
        if (stats.stations > 0) stats.stations else AppConstants.DEFAULT_MAX_STATIONS
    } catch (_: Exception) {
        AppConstants.DEFAULT_MAX_STATIONS
    }

    // ===== 统计 =====

    /** 从 /json/stats 获取平台统计（带内存缓存） */
    suspend fun loadStats(forceRefresh: Boolean = false): RadioStats {
        if (!forceRefresh) {
            cachedStats?.let { return it }
        }
        val stats = api.stats().toModel()
        cachedStats = stats
        return stats
    }

    /** 从本地缓存统计电台/国家/语言/标签数量（不发网络请求） */
    suspend fun loadLocalStats(): RadioStats {
        val stations = loadStations()
        val countries = mutableSetOf<String>()
        val languages = mutableSetOf<String>()
        val tags = mutableSetOf<String>()
        for (s in stations) {
            if (s.country.isNotEmpty()) countries.add(s.country)
            if (s.language.isNotEmpty()) languages.add(s.language)
            if (s.category.isNotEmpty()) tags.add(s.category)
        }
        return RadioStats(
            stations = stations.size,
            clicks = 0,
            countries = countries.size,
            languages = languages.size,
            tags = tags.size,
            clicksLastHour = 0,
            stationsBroken = 0,
        )
    }

    // ===== 维度查询 =====

    /** 按 ISO 国家代码加载电台 */
    suspend fun loadByCountry(countryCode: String): List<Station> = try {
        api.byCountryCode(countryCode).toValidStations()
    } catch (_: Exception) {
        emptyList()
    }

    /** 按国家名称精确加载（用于推荐页国家偏好） */
    suspend fun loadByCountryName(countryName: String): List<Station> = try {
        api.byCountryExact(countryName, hidebroken = "true").toValidStations()
    } catch (_: Exception) {
        emptyList()
    }

    /** 最近活跃的电台（按 lastchecktime 排序） */
    suspend fun loadNewestStations(limit: Int = 20): List<Station> = try {
        api.stations(
            limit = limit, order = "lastchecktime",
            reverse = "true", hidebroken = "true",
        ).toValidStations()
    } catch (_: Exception) {
        emptyList()
    }

    /** 按标签加载电台 */
    suspend fun loadByTag(tag: String): List<Station> = try {
        api.byTag(tag).toValidStations()
    } catch (_: Exception) {
        emptyList()
    }

    /** 按语言加载电台 */
    suspend fun loadByLanguage(language: String): List<Station> = try {
        api.byLanguage(language).toValidStations()
    } catch (_: Exception) {
        emptyList()
    }

    /** 通过 API 按名称搜索 */
    suspend fun searchStationsRemote(query: String): List<Station> = try {
        api.byName(query).toValidStations()
    } catch (_: Exception) {
        emptyList()
    }

    /** 从本地缓存搜索（名称/国家/语言/分类/标签描述） */
    suspend fun searchCachedStations(keyword: String): List<Station> =
        cache.search(keyword)

    // ===== 维度列表（从本地缓存统计，与原版一致）=====

    /** 从缓存数据统计国家列表（按电台数降序） */
    suspend fun loadCountries(): List<Country> {
        val stations = loadStations()
        if (stations.isEmpty()) return emptyList()
        val counts = LinkedHashMap<String, Int>()
        val codes = HashMap<String, String>()
        for (s in stations) {
            if (s.country.isEmpty()) continue
            counts[s.country] = (counts[s.country] ?: 0) + 1
            if (s.countryCode.isNotEmpty() && !codes.containsKey(s.country)) {
                codes[s.country] = s.countryCode
            }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .map { Country(it.key, codes[it.key].orEmpty(), it.value) }
    }

    /** 从缓存数据统计标签列表 */
    suspend fun loadTags(): List<Tag> {
        val stations = loadStations()
        if (stations.isEmpty()) return emptyList()
        val counts = LinkedHashMap<String, Int>()
        for (s in stations) {
            if (s.category.isEmpty()) continue
            counts[s.category] = (counts[s.category] ?: 0) + 1
        }
        return counts.entries
            .sortedByDescending { it.value }
            .map { Tag(it.key, it.value) }
    }

    /** 从缓存数据统计语言列表 */
    suspend fun loadLanguages(): List<Language> {
        val stations = loadStations()
        if (stations.isEmpty()) return emptyList()
        val counts = LinkedHashMap<String, Int>()
        for (s in stations) {
            if (s.language.isEmpty()) continue
            counts[s.language] = (counts[s.language] ?: 0) + 1
        }
        return counts.entries
            .sortedByDescending { it.value }
            .map { Language(it.key, it.value) }
    }

    // ===== 缓存管理 =====

    suspend fun getCachedStationCount(): Int = cache.getCachedCount()

    suspend fun clearCache() {
        cachedStats = null
        cache.clearCache()
    }
}
