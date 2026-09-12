package com.huanglongmao.onlinefmradio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 电台数据模型（对应 Flutter 版 station.dart）。
 *
 * 同时承担两种职责：
 * - 业务领域对象
 * - 本地缓存 / 收藏 / 历史 / 导入导出 的 JSON 格式（camelCase 字段）
 */
@Serializable
data class Station(
    val id: String = "",
    val name: String = "",
    val streamUrl: String = "",
    val country: String = "",
    val countryCode: String = "",
    val language: String = "",
    val category: String = "",
    val logo: String = "",
    val description: String = "",
    val votes: Int = 0,
    val bitrate: Int = 0,
    val codec: String = "",
) {
    /** 将 HTTP logo URL 升级为 HTTPS，避免混合内容限制 */
    val safeLogo: String
        get() = when {
            logo.isEmpty() -> ""
            logo.startsWith("https://") -> logo
            logo.startsWith("http://") -> "https://${logo.substring(7)}"
            else -> logo
        }

    /** 基于 streamUrl / logo 域名生成的 Google Favicon 备用 URL */
    val faviconFallback: String
        get() {
            val src = streamUrl.ifEmpty { logo }
            val host = try {
                java.net.URI(src).host
            } catch (_: Exception) {
                null
            }
            return if (host.isNullOrEmpty()) ""
            else "https://www.google.com/s2/favicons?domain=$host&sz=128"
        }

    /** 根据 countryCode (ISO 3166-1 alpha-2) 生成国旗 emoji */
    val flagEmoji: String
        get() {
            if (countryCode.length != 2) return ""
            val upper = countryCode.uppercase()
            val base = 0x1F1E6
            val first = base + (upper[0].code - 0x41)
            val second = base + (upper[1].code - 0x41)
            return String(Character.toChars(first)) + String(Character.toChars(second))
        }
}

/**
 * radio-browser.info API 返回的电台 JSON 结构（snake_case 字段）。
 */
@Serializable
data class StationDto(
    @SerialName("stationuuid") val stationUuid: String? = null,
    val name: String? = null,
    val url: String? = null,
    @SerialName("url_resolved") val urlResolved: String? = null,
    val country: String? = null,
    @SerialName("countrycode") val countryCode: String? = null,
    val language: String? = null,
    val favicon: String? = null,
    val tags: String? = null,
    val votes: Int = 0,
    val bitrate: Int = 0,
    val codec: String? = null,
) {
    /** 转换为领域模型；无效数据（无 uuid / 流地址 / 名称）返回 null */
    fun toStation(): Station? {
        val uuid = stationUuid ?: return null
        if (uuid.isEmpty()) return null
        val stream = urlResolved?.takeIf { it.isNotEmpty() } ?: url ?: return null
        if (stream.isEmpty()) return null
        val trimmedName = name?.trim().orEmpty()
        if (trimmedName.isEmpty()) return null

        val tagList = tags?.split(',')?.map { it.trim() } ?: emptyList()
        val category = tagList.firstOrNull()?.takeIf { it.isNotEmpty() } ?: "Other"

        return Station(
            id = uuid,
            name = trimmedName,
            streamUrl = stream,
            country = country ?: "Unknown",
            countryCode = countryCode ?: "",
            language = language?.trim().orEmpty(),
            category = category,
            logo = favicon ?: "",
            description = tagList.joinToString(", "),
            votes = votes,
            bitrate = bitrate,
            codec = codec ?: "",
        )
    }
}
