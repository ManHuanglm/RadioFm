package com.huanglongmao.onlinefmradio.data.cache

import android.content.Context
import com.huanglongmao.onlinefmradio.data.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 电台本地文件缓存（对应 Flutter 版 station_cache_service.dart）。
 *
 * 原 Flutter 版因 Web 兼容使用 SharedPreferences 分块 JSON；原生版直接使用文件：
 * - `stations_cache.jsonl`：每行一个 Station JSON，追加写入 O(1)
 * - 内存维护 LinkedHashMap 索引（按插入序），追加前按 ID 去重，文件内不会产生重复行
 * - 首次访问时流式加载一次；全部 IO 走 Dispatchers.IO
 */
class StationFileCache(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = false
    }
    private val listSerializer = ListSerializer(Station.serializer())

    private val file = File(context.filesDir, "stations_cache.jsonl")
    private val mutex = Mutex()

    private val byId = LinkedHashMap<String, Station>()
    private var loaded = false

    /** 确保内存索引已从文件加载（只加载一次） */
    private suspend fun ensureLoaded() = mutex.withLock {
        if (loaded) return@withLock
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    file.useLines { lines ->
                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.isEmpty()) continue
                            runCatching { json.decodeFromString(Station.serializer(), trimmed) }
                                .getOrNull()
                                ?.let { synchronized(byId) { byId[it.id] = it } }
                        }
                    }
                }
            } catch (_: Exception) {
                // 缓存损坏时按空缓存处理，等待下次写入重建
                synchronized(byId) { byId.clear() }
            }
            loaded = true
        }
    }

    /** 是否存在缓存 */
    suspend fun hasCache(): Boolean {
        ensureLoaded()
        return byId.isNotEmpty()
    }

    /** 获取全部缓存电台（按插入序） */
    suspend fun getAll(): List<Station> {
        ensureLoaded()
        synchronized(byId) { return byId.values.toList() }
    }

    /** 缓存数量 */
    suspend fun getCachedCount(): Int {
        ensureLoaded()
        synchronized(byId) { return byId.size }
    }

    /** 已缓存的电台 ID 集合（用于全量更新的差异对比） */
    suspend fun getCachedIds(): Set<String> {
        ensureLoaded()
        synchronized(byId) { return byId.keys.toSet() }
    }

    /**
     * 追加电台（自动跳过已存在的 ID）。
     * 文件以 APPEND 模式逐行写入，只有新增数据落盘。
     */
    suspend fun appendStations(stations: List<Station>) {
        if (stations.isEmpty()) return
        ensureLoaded()
        val newOnes = ArrayList<Station>(stations.size)
        synchronized(byId) {
            for (s in stations) {
                if (s.id.isEmpty() || byId.containsKey(s.id)) continue
                byId[s.id] = s
                newOnes.add(s)
            }
        }
        if (newOnes.isEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                file.appendText(newOnes.joinToString("") { s ->
                    json.encodeToString(Station.serializer(), s) + "\n"
                })
            } catch (_: Exception) {
                // 写盘失败不影响内存态，下次启动重新校准
            }
        }
    }

    /** 整表替换（首次从 API 加载 30 条时的缓存初始化） */
    suspend fun replaceAll(stations: List<Station>) {
        ensureLoaded()
        synchronized(byId) {
            byId.clear()
            for (s in stations) {
                if (s.id.isNotEmpty()) byId[s.id] = s
            }
        }
        withContext(Dispatchers.IO) {
            try {
                file.writeText(stations.joinToString("") { s ->
                    json.encodeToString(Station.serializer(), s) + "\n"
                })
            } catch (_: Exception) {
            }
        }
    }

    /** 清空缓存 */
    suspend fun clearCache() {
        ensureLoaded()
        synchronized(byId) { byId.clear() }
        withContext(Dispatchers.IO) {
            try {
                file.delete()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 从本地缓存中搜索电台。
     * 范围：名称、国家、语言、分类、标签描述。
     */
    suspend fun search(keyword: String): List<Station> {
        val trimmed = keyword.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()
        val all = getAll()
        return all.filter { s ->
            s.name.lowercase().contains(trimmed) ||
                s.country.lowercase().contains(trimmed) ||
                s.language.lowercase().contains(trimmed) ||
                s.category.lowercase().contains(trimmed) ||
                s.description.lowercase().contains(trimmed)
        }
    }
}
