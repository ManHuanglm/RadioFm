package com.huanglongmao.onlinefmradio.data.cache

import android.content.Context
import com.huanglongmao.onlinefmradio.data.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * assets/data/stations.json 兜底数据源
 * （对应 Flutter 版 local_station_datasource._loadFromLocalAsset）。
 *
 * API 不可用且无缓存时的最后防线。
 */
class AssetFallback(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Volatile
    private var cached: List<Station>? = null

    suspend fun load(): List<Station> = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        val result = runCatching {
            val text = context.assets.open("data/stations.json").bufferedReader().use { it.readText() }
            json.decodeFromString(ListSerializer(Station.serializer()), text)
        }.getOrElse { emptyList() }
        cached = result
        result
    }
}
