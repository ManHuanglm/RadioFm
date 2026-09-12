package com.huanglongmao.onlinefmradio.store

import android.content.Context
import android.net.Uri
import com.huanglongmao.onlinefmradio.data.model.Station
import com.huanglongmao.onlinefmradio.data.model.StationDto
import com.huanglongmao.onlinefmradio.data.remote.RadioBrowserApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** 导出筛选条件（对应 Flutter 版 ExportFilter） */
data class ExportFilter(
    val limit: Int = 100,
    val countryCode: String? = null,
    val countryName: String? = null,
    val tag: String? = null,
    val language: String? = null,
    val hideBroken: Boolean = true,
) {
    val hasFilter: Boolean
        get() = !countryCode.isNullOrEmpty() || !countryName.isNullOrEmpty() ||
            !tag.isNullOrEmpty() || !language.isNullOrEmpty()

    val description: String
        get() {
            val parts = mutableListOf("数量: $limit")
            if (!countryCode.isNullOrEmpty()) parts.add("国家码: $countryCode")
            if (!countryName.isNullOrEmpty()) parts.add("国家: $countryName")
            if (!tag.isNullOrEmpty()) parts.add("标签: $tag")
            if (!language.isNullOrEmpty()) parts.add("语言: $language")
            return parts.joinToString(" | ")
        }
}

/**
 * 导入导出管理器（对应 Flutter 版 import_export_service.dart）。
 *
 * - m3u/m3u8/json 解析与生成（文件读写走 SAF，由 UI 提供 Uri）
 * - 按筛选条件从远程 API 获取电台用于导出
 */
class ImportExportManager(private val api: RadioBrowserApi) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val listSerializer = ListSerializer(Station.serializer())

    // ===== 解析 =====

    /** 根据文件名分派解析（m3u/m3u8 → M3U 解析器，json → JSON 解析器） */
    fun parse(fileName: String, content: String): List<Station> {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".m3u") || lower.endsWith(".m3u8") -> parseM3U(content)
            lower.endsWith(".json") -> parseJson(content)
            else -> emptyList()
        }
    }

    /** 解析 M3U/M3U8：#EXTINF 行为名称，紧随的 URL 行为流地址 */
    fun parseM3U(content: String): List<Station> {
        val stations = mutableListOf<Station>()
        var name: String? = null
        for (raw in content.lines()) {
            val line = raw.trim()
            when {
                line.isEmpty() || line.startsWith("#EXTM3U") -> Unit
                line.startsWith("#EXTINF:") -> {
                    val idx = line.indexOf(',')
                    name = if (idx >= 0 && idx < line.length - 1) {
                        line.substring(idx + 1).trim()
                    } else "Unknown"
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    val n = name
                    if (n != null) {
                        stations.add(
                            Station(
                                id = line.hashCode().toString(),
                                name = n,
                                streamUrl = line,
                                category = "Imported",
                            ),
                        )
                        name = null
                    }
                }
            }
        }
        return stations
    }

    /** 解析 JSON：Station 数组 */
    fun parseJson(content: String): List<Station> = runCatching {
        json.decodeFromString(listSerializer, content)
    }.getOrElse { emptyList() }

    // ===== 生成 =====

    fun generate(stations: List<Station>, format: String): String = when (format) {
        "m3u" -> generateM3U(stations)
        "m3u8" -> generateM3U8(stations)
        else -> generateJson(stations)
    }

    fun generateM3U(stations: List<Station>): String = buildString {
        appendLine("#EXTM3U")
        for (s in stations) {
            appendLine("#EXTINF:-1,${s.name}")
            appendLine(s.streamUrl)
        }
    }

    fun generateM3U8(stations: List<Station>): String = buildString {
        appendLine("#EXTM3U")
        for (s in stations) {
            appendLine("#EXTINF:-1 group-title=\"${s.category}\",${s.name}")
            appendLine("#EXTGRP:${s.country}")
            appendLine(s.streamUrl)
        }
    }

    fun generateJson(stations: List<Station>): String =
        json.encodeToString(listSerializer, stations)

    // ===== 文件读写（SAF）=====

    /** 从 SAF Uri 读取内容并解析 */
    suspend fun importFromUri(context: Context, uri: Uri): List<Station> =
        withContext(Dispatchers.IO) {
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            } ?: "stations.m3u"
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull() ?: return@withContext emptyList()
            parse(fileName, content)
        }

    /** 将内容写入 SAF Uri（导出） */
    suspend fun exportToUri(context: Context, uri: Uri, content: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                    out.flush()
                } != null
            }.getOrElse { false }
        }

    // ===== 远程获取（开发调试导出）=====

    /** 按筛选条件从远程获取电台列表 */
    suspend fun fetchStationsByFilter(filter: ExportFilter): List<Station> =
        withContext(Dispatchers.IO) {
            api.stationsFiltered(
                limit = filter.limit,
                hidebroken = filter.hideBroken.toString(),
                countryCode = filter.countryCode?.takeIf { it.isNotEmpty() },
                country = filter.countryName?.takeIf { it.isNotEmpty() },
                tag = filter.tag?.takeIf { it.isNotEmpty() },
                language = filter.language?.takeIf { it.isNotEmpty() },
            ).mapNotNull(StationDto::toStation).filter { it.streamUrl.isNotEmpty() }
        }
}
