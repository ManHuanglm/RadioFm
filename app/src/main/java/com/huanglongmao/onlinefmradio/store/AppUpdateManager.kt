package com.huanglongmao.onlinefmradio.store

import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import com.huanglongmao.onlinefmradio.data.model.ReleaseNote
import com.huanglongmao.onlinefmradio.data.model.toReleaseNote
import com.huanglongmao.onlinefmradio.data.remote.GithubApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 应用版本更新管理器（对应 Flutter 版 app_update_service.dart）。
 *
 * - GitHub Releases 拉取（24 小时最小检查间隔，强制刷新除外）
 * - 版本列表缓存到 DataStore（离线可用）
 * - 跳过指定版本
 */
class AppUpdateManager(
    private val api: GithubApi,
    private val settings: SettingsDataStore,
) {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** 检查是否已超过最小检查间隔（24h） */
    suspend fun shouldCheck(): Boolean {
        val last = settings.getLong(KEY_LAST_CHECK) ?: 0L
        return System.currentTimeMillis() - last >= AppConstants.UPDATE_CHECK_MIN_INTERVAL_MS
    }

    /**
     * 检查更新。
     * [force] 为 true 时忽略 24h 间隔强制联网；失败时回退本地缓存。
     * 返回按版本降序的发布列表；无数据返回空列表。
     */
    suspend fun checkForUpdates(force: Boolean = false): List<ReleaseNote> = withContext(Dispatchers.IO) {
        val cached = loadCachedReleases()
        if (!force && !shouldCheck()) return@withContext cached

        val remote = runCatching {
            api.releases(AppConstants.GITHUB_RELEASES_API_URL)
                .map { it.toReleaseNote() }
        }.getOrElse { return@withContext cached }

        if (remote.isEmpty()) return@withContext cached.ifEmpty { remote }

        persistReleases(remote)
        markChecked()
        remote
    }

    /** 从列表中取最新正式版 */
    fun latestStable(releases: List<ReleaseNote>): ReleaseNote? =
        releases.firstOrNull { !it.prerelease }

    /** 当前版本是否有新版本可升级 */
    fun isNewer(release: ReleaseNote): Boolean {
        if (release.normalizedVersion.isEmpty()) return false
        return ReleaseNote.compareVersions(
            release.normalizedVersion,
            ReleaseNote.normalizeVersion(AppConstants.APP_VERSION),
        ) > 0
    }

    /** 该版本是否被用户选择跳过 */
    suspend fun isSkipped(version: String): Boolean {
        if (version.isEmpty()) return false
        return settings.getString(AppConstants.KEY_APP_UPDATE_SKIPPED_VERSION) == version
    }

    /** 跳过指定版本 */
    suspend fun skipVersion(version: String) {
        runCatching { settings.putString(AppConstants.KEY_APP_UPDATE_SKIPPED_VERSION, version) }
    }

    // ===== 缓存 =====

    private suspend fun loadCachedReleases(): List<ReleaseNote> {
        val raw = settings.getString(AppConstants.KEY_APP_UPDATE_CACHED_RELEASES) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(ReleaseNote.serializer()), raw)
        }.getOrElse { emptyList() }
    }

    private suspend fun persistReleases(releases: List<ReleaseNote>) {
        val trimmed = releases.take(AppConstants.MAX_CACHED_RELEASES)
        runCatching {
            settings.putString(
                AppConstants.KEY_APP_UPDATE_CACHED_RELEASES,
                json.encodeToString(ListSerializer(ReleaseNote.serializer()), trimmed),
            )
        }
    }

    private suspend fun markChecked() {
        runCatching { putLastCheck(System.currentTimeMillis()) }
    }

    private suspend fun putLastCheck(time: Long) {
        settings.putString(KEY_LAST_CHECK, time.toString())
    }

    companion object {
        /** 复用字符串键存时间戳（与原版字符串存储对齐） */
        private const val KEY_LAST_CHECK = "app_update_last_check_long"
    }
}

/** DataStore 辅助：字符串形式存储的 long 值 */
private suspend fun SettingsDataStore.getLong(key: String): Long? =
    getString(key)?.toLongOrNull()
