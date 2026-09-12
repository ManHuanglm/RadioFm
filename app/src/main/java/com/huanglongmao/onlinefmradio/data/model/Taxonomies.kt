package com.huanglongmao.onlinefmradio.data.model

import kotlinx.serialization.Serializable

/** 国家（可来自 API 或本地缓存统计） */
@Serializable
data class Country(
    val name: String = "",
    val countryCode: String = "",
    val stationCount: Int = 0,
)

/** 语言 */
@Serializable
data class Language(
    val name: String = "",
    val stationCount: Int = 0,
)

/** 标签（电台分类） */
@Serializable
data class Tag(
    val name: String = "",
    val stationCount: Int = 0,
)

/** 电台统计数据（对应 /json/stats） */
@Serializable
data class RadioStats(
    val stations: Int = 0,
    val clicks: Int = 0,
    val countries: Int = 0,
    val languages: Int = 0,
    val tags: Int = 0,
    val clicksLastHour: Int = 0,
    val stationsBroken: Int = 0,
)

/** /json/stats API 原始结构 */
@kotlinx.serialization.Serializable
data class RadioStatsDto(
    val stations: Int = 0,
    val clicks: Int = 0,
    val countries: Int = 0,
    val languages: Int = 0,
    val tags: Int = 0,
    @kotlinx.serialization.SerialName("clicks_last_hour") val clicksLastHour: Int = 0,
    @kotlinx.serialization.SerialName("stationsbroken") val stationsBroken: Int = 0,
) {
    fun toModel() = RadioStats(
        stations = stations,
        clicks = clicks,
        countries = countries,
        languages = languages,
        tags = tags,
        clicksLastHour = clicksLastHour,
        stationsBroken = stationsBroken,
    )
}

/** countries / languages / tags 列表端点的通用结构 */
@kotlinx.serialization.Serializable
data class NameCountDto(
    val name: String = "",
    @kotlinx.serialization.SerialName("iso_3166_1") val iso31661: String? = null,
    @kotlinx.serialization.SerialName("stationcount") val stationCount: Int = 0,
)
