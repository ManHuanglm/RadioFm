package com.huanglongmao.onlinefmradio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 版本发布记录（对应 Flutter 版 release_note.dart）。
 * 序列化字段与 Flutter 版一致，便于对齐缓存格式。
 */
@Serializable
data class ReleaseNote(
    val version: String = "",
    val normalizedVersion: String = "",
    val publishedAt: String = "",
    val body: String = "",
    val htmlUrl: String = "",
    val prerelease: Boolean = false,
    val isLatest: Boolean = false,
    val apkDownloadUrl: String? = null,
) {
    companion object {
        /** 将 "v1.0.1" / "release-1.0.1" 等前缀剥离为纯版本号 */
        fun normalizeVersion(tag: String): String {
            var s = tag.trim()
            if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1)
            return s
        }

        /**
         * 比较两个 normalized 版本号：>0 表示 a 较新，<0 表示 b 较新，0 相等。
         * 按点分段逐段比较数字大小；非数字段按字符串字典序。
         */
        fun compareVersions(a: String, b: String): Int {
            val pa = a.split('.')
            val pb = b.split('.')
            val len = maxOf(pa.size, pb.size)
            for (i in 0 until len) {
                val sa = pa.getOrElse(i) { "0" }
                val sb = pb.getOrElse(i) { "0" }
                val na = sa.toIntOrNull()
                val nb = sb.toIntOrNull()
                if (na != null && nb != null) {
                    if (na != nb) return na - nb
                } else {
                    val cmp = sa.compareTo(sb)
                    if (cmp != 0) return cmp
                }
            }
            return 0
        }
    }
}

/**
 * 静态更新清单（update.json，方案 2：自维护版本清单托管于 GitHub Pages / OSS）。
 *
 * 结构示例：
 * ```json
 * {
 *   "releases": [
 *     { "version": "v1.0.1", "publishedAt": "2026-09-12", "body": "...",
 *       "apkUrl": "https://.../app-release.apk", "prerelease": false }
 *   ]
 * }
 * ```
 */
@Serializable
data class UpdateManifestDto(
    val releases: List<UpdateReleaseDto> = emptyList(),
)

/** 清单中的单条版本记录 */
@Serializable
data class UpdateReleaseDto(
    val version: String = "",
    val publishedAt: String = "",
    val body: String = "",
    val apkUrl: String? = null,
    val htmlUrl: String = "",
    val prerelease: Boolean = false,
)

/** UpdateReleaseDto → ReleaseNote */
fun UpdateReleaseDto.toReleaseNote(isLatest: Boolean = false): ReleaseNote = ReleaseNote(
    version = version,
    normalizedVersion = ReleaseNote.normalizeVersion(version),
    publishedAt = publishedAt,
    body = body,
    htmlUrl = htmlUrl,
    prerelease = prerelease,
    isLatest = isLatest,
    apkDownloadUrl = apkUrl,
)

/**
 * GitHub Releases API 单条发布记录（只需用到的字段）。
 */
@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    val body: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList(),
)

/** GitHub Release 附件（APK 安装包） */
@Serializable
data class GitHubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
)

/** GitHubReleaseDto → ReleaseNote（APK 优先选 universal 包，其次任意 .apk） */
fun GitHubReleaseDto.toReleaseNote(isLatest: Boolean = false): ReleaseNote {
    val apk = assets.firstOrNull {
        it.name.endsWith(".apk", ignoreCase = true) && it.name.contains("universal", ignoreCase = true)
    }?.browserDownloadUrl
        ?: assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }?.browserDownloadUrl
    return ReleaseNote(
        version = tagName,
        normalizedVersion = ReleaseNote.normalizeVersion(tagName),
        publishedAt = publishedAt,
        body = body,
        htmlUrl = htmlUrl,
        prerelease = prerelease,
        isLatest = isLatest,
        apkDownloadUrl = apk,
    )
}
