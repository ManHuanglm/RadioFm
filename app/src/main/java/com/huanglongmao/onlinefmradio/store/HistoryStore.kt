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
 * 播放历史服务（对应 Flutter 版 history_service.dart）。
 * 存储：play_history 键；去重后插头部；上限 10。
 */
class HistoryStore(private val settings: SettingsDataStore) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val serializer = ListSerializer(Station.serializer())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _history = MutableStateFlow<List<Station>>(emptyList())
    val history: StateFlow<List<Station>> = _history

    /** 启动时加载 */
    suspend fun load() {
        mutex.withLock {
            val raw = settings.getString(AppConstants.KEY_PLAY_HISTORY)
            val list = raw?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrElse { emptyList() }
            } ?: emptyList()
            _history.value = list
        }
    }

    /** 添加历史：移除旧的同 ID 记录后插入头部，超出上限移除最旧 */
    suspend fun addToHistory(station: Station) {
        if (station.id.isEmpty()) return
        mutex.withLock {
            val current = _history.value.toMutableList()
            current.removeAll { it.id == station.id }
            current.add(0, station)
            while (current.size > AppConstants.MAX_HISTORY_LENGTH) {
                current.removeAt(current.size - 1)
            }
            _history.value = current
            runCatching {
                settings.putString(
                    AppConstants.KEY_PLAY_HISTORY,
                    json.encodeToString(serializer, current),
                )
            }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            _history.value = emptyList()
            runCatching { settings.putString(AppConstants.KEY_PLAY_HISTORY, null) }
        }
    }

    init {
        scope.launch { load() }
    }
}
