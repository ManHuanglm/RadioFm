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

/** GitHub Releases API 的单条 release */
@Serializable
data class GithubReleaseDto(
    @SerialName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val prerelease: Boolean = false,
    val assets: List<GithubAssetDto> = emptyList(),
)

/** GitHub release 的资产文件 */
@Serializable
data class GithubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
    val url: String? = null,
) {
    val apkDownloadUrl: String?
        get() = if (name.lowercase().endsWith(".apk")) {
            browserDownloadUrl ?: url
        } else null
}

/** GithubReleaseDto → ReleaseNote */
fun GithubReleaseDto.toReleaseNote(isLatest: Boolean = false): ReleaseNote {
    val tag = tagName.orEmpty()
    return ReleaseNote(
        version = tag,
        normalizedVersion = ReleaseNote.normalizeVersion(tag),
        publishedAt = publishedAt ?: createdAt.orEmpty(),
        body = body.orEmpty(),
        htmlUrl = htmlUrl.orEmpty(),
        prerelease = prerelease,
        isLatest = isLatest,
        apkDownloadUrl = assets.firstNotNullOfOrNull { it.apkDownloadUrl },
    )
}
