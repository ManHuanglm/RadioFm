package com.huanglongmao.onlinefmradio.store

import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * 电台播放失败计数服务。
 *
 * 每个电台 ID 对应累计失败次数（重连耗尽计一次）；播放成功时清零；
 * 达到 [AppConstants.MAX_PLAY_FAILURES] 后由 PlayerController 判定为故障电台并自动切台。
 * 持久化为 JSON map 存于 DataStore，跨会话保留。
 */
class PlayFailureStore(private val settings: SettingsDataStore) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val serializer = MapSerializer(String.serializer(), Int.serializer())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counts: StateFlow<Map<String, Int>> = _counts

    private var loaded = false

    /** 启动时加载（幂等，可重复调用） */
    suspend fun load() {
        mutex.withLock {
            val raw = settings.getString(AppConstants.KEY_PLAY_FAILURE_COUNTS)
            _counts.value = raw?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrElse { emptyMap() }
            } ?: emptyMap()
            loaded = true
        }
    }

    /** 累加某电台的失败次数，返回累加后的值 */
    suspend fun incrementFailure(stationId: String): Int {
        if (stationId.isEmpty()) return 0
        mutex.withLock {
            if (!loaded) load()
            val updated = _counts.value.toMutableMap()
            val next = (updated[stationId] ?: 0) + 1
            updated[stationId] = next
            _counts.value = updated
            persist(updated)
            return next
        }
    }

    /** 播放成功后清零该电台的失败计数 */
    suspend fun resetFailure(stationId: String) {
        if (stationId.isEmpty()) return
        mutex.withLock {
            if (!loaded) load()
            if (!_counts.value.containsKey(stationId)) return
            val updated = _counts.value.toMutableMap().apply { remove(stationId) }
            _counts.value = updated
            persist(updated)
        }
    }

    private suspend fun persist(map: Map<String, Int>) {
        runCatching {
            settings.putString(
                AppConstants.KEY_PLAY_FAILURE_COUNTS,
                json.encodeToString(serializer, map),
            )
        }
    }

    init {
        scope.launch { load() }
    }
}
