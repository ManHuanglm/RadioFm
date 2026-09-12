package com.huanglongmao.onlinefmradio.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.huanglongmao.onlinefmradio.core.network.RetrofitFactory
import com.huanglongmao.onlinefmradio.data.model.Station

/**
 * Station ↔ MediaItem 互转（对应 Flutter 版 audio_handler._stationToMediaItem）。
 *
 * 完整电台对象以 JSON 形式放入 extras，跨进程往返无损；
 * 直播流 duration 保持 TIME_UNSET，通知栏与 UI 不显示进度条。
 */
object MediaItemMapper {

    private const val EXTRA_STATION = "station_json"
    private val json = RetrofitFactory.json

    fun toMediaItem(station: Station): MediaItem {
        val extras = android.os.Bundle().apply {
            putString(EXTRA_STATION, json.encodeToString(Station.serializer(), station))
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.country)
            .setAlbumTitle(station.country)
            .setGenre(station.category)
            .setDescription(station.description)
            .setArtworkUri(
                station.safeLogo.ifEmpty { null }?.let { android.net.Uri.parse(it) }
            )
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(station.streamUrl)
            .setMediaMetadata(metadata)
            .setMimeType(guessMimeType(station))
            .build()
    }

    /** 从 MediaItem 还原 Station；无 extras 时构造最小可用对象 */
    fun toStation(item: MediaItem?): Station? {
        if (item == null) return null
        val raw = item.mediaMetadata.extras?.getString(EXTRA_STATION)
        if (raw != null) {
            return runCatching { json.decodeFromString(Station.serializer(), raw) }.getOrNull()
        }
        // 兜底：从媒体元数据重建（应仅发生在 extras 被丢弃的异常路径）
        return Station(
            id = item.mediaId,
            name = item.mediaMetadata.title?.toString().orEmpty(),
            streamUrl = item.localConfiguration?.uri?.toString().orEmpty(),
            country = item.mediaMetadata.artist?.toString().orEmpty(),
            category = item.mediaMetadata.genre?.toString().orEmpty(),
        )
    }

    /**
     * MIME 类型提示：HLS 流交给 HLS 模块处理，其余交给 ExoPlayer 自动探测。
     */
    private fun guessMimeType(station: Station): String? = when {
        station.streamUrl.contains(".m3u8") -> "application/x-mpegURL"
        station.codec.contains("AAC", ignoreCase = true) -> androidx.media3.common.MimeTypes.AUDIO_AAC
        else -> null
    }
}
