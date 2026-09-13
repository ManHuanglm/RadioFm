package com.huanglongmao.onlinefmradio.store

import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.data.model.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 本地电台服务（对应 Flutter 版 local_station_service.dart）。
 * 管理通过 m3u/m3u8/json 导入的本地电台，与远程缓存相互独立。
 */
class LocalStationStore(private val settings: SettingsDataStore) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val serializer = ListSerializer(Station.serializer())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations

    /** 启动时加载（顺带清理存量重复：ID 或流地址相同视为同一电台） */
    suspend fun load() {
        mutex.withLock {
            val raw = settings.getString(AppConstants.KEY_LOCAL_STATIONS)
            val list = raw?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrElse { emptyList() }
            } ?: emptyList()
            val deduped = dedupe(list)
            if (deduped.size != list.size) persist(deduped)
            _stations.value = deduped
        }
    }

    /** 导入电台列表（按 ID 与流地址双重去重），返回新增数量 */
    suspend fun importStations(stations: List<Station>): Int {
        var added = 0
        mutex.withLock {
            val current = _stations.value.toMutableList()
            val existingIds = current.mapTo(HashSet()) { it.id }
            val existingUrls = current.mapTo(HashSet()) { it.streamUrl.trim().lowercase() }
            for (station in stations) {
                if (station.id.isEmpty() && station.streamUrl.isEmpty()) continue
                val url = station.streamUrl.trim().lowercase()
                val duplicate = (station.id.isNotEmpty() && station.id in existingIds) ||
                    (url.isNotEmpty() && url in existingUrls)
                if (!duplicate) {
                    current.add(station)
                    added++
                    if (station.id.isNotEmpty()) existingIds.add(station.id)
                    if (url.isNotEmpty()) existingUrls.add(url)
                }
            }
            _stations.value = current
            persist(current)
        }
        return added
    }

    /** 保留首次出现，剔除 ID 或流地址重复的条目 */
    private fun dedupe(list: List<Station>): List<Station> {
        val seenIds = HashSet<String>()
        val seenUrls = HashSet<String>()
        return list.filter { s ->
            val url = s.streamUrl.trim().lowercase()
            val idDup = s.id.isNotEmpty() && !seenIds.add(s.id)
            val urlDup = url.isNotEmpty() && !seenUrls.add(url)
            !idDup && !urlDup
        }
    }

    /** 移除单个本地电台 */
    suspend fun removeStation(station: Station) {
        mutex.withLock {
            val current = _stations.value.toMutableList()
            current.removeAll { it.id == station.id }
            _stations.value = current
            persist(current)
        }
    }

    /** 清空所有本地电台 */
    suspend fun clearAll() {
        mutex.withLock {
            _stations.value = emptyList()
            runCatching { settings.putString(AppConstants.KEY_LOCAL_STATIONS, null) }
        }
    }

    private suspend fun persist(list: List<Station>) {
        runCatching {
            settings.putString(
                AppConstants.KEY_LOCAL_STATIONS,
                json.encodeToString(serializer, list),
            )
        }
    }

    init {
        scope.launch { load() }
    }
}
