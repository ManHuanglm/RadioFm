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
 * 收藏服务（对应 Flutter 版 favorites_service.dart）。
 * 存储：favorites 键，JSON 数组字符串；新收藏插头部；上限 50。
 */
class FavoritesStore(private val settings: SettingsDataStore) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val serializer = ListSerializer(Station.serializer())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _favorites = MutableStateFlow<List<Station>>(emptyList())

    /** 收藏列表（新收藏在前） */
    val favorites: StateFlow<List<Station>> = _favorites

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())

    /** 收藏 ID 集合（供列表页 O(1) 判断） */
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    /** 启动时加载 */
    suspend fun load() {
        mutex.withLock {
            val raw = settings.getString(AppConstants.KEY_FAVORITES)
            val list = raw?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrElse { emptyList() }
            } ?: emptyList()
            _favorites.value = list
            _favoriteIds.value = list.map { it.id }.toSet()
        }
    }

    /** 切换收藏状态：已收藏则移除，未收藏则插入头部 */
    suspend fun toggle(station: Station): Boolean {
        var added = false
        mutex.withLock {
            val current = _favorites.value.toMutableList()
            val exists = current.any { it.id == station.id }
            if (exists) {
                current.removeAll { it.id == station.id }
            } else {
                current.add(0, station)
                if (current.size > AppConstants.MAX_FAVORITE_STATIONS) {
                    current.removeAt(current.size - 1)
                }
                added = true
            }
            persist(current)
            _favorites.value = current
            _favoriteIds.value = current.map { it.id }.toSet()
        }
        return added
    }

    suspend fun isFavorite(id: String): Boolean = _favoriteIds.value.contains(id)

    private suspend fun persist(list: List<Station>) {
        runCatching {
            settings.putString(AppConstants.KEY_FAVORITES, json.encodeToString(serializer, list))
        }
    }

    init {
        scope.launch { load() }
    }
}
