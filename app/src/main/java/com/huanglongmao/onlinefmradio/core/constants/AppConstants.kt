package com.huanglongmao.onlinefmradio.core.constants

/// 应用全局常量配置（对应 Flutter 版 app_constants.dart）
object AppConstants {
    const val APP_NAME = "听电台"
    const val APP_VERSION = "1.0.0"
    const val APP_BUILD_NUMBER = "1"
    const val PACKAGE_NAME = "com.huanglongmao.onlinefmradio"

    // ===== 应用更新（方案 2：静态 update.json 版本清单）=====

    const val GITHUB_OWNER = "ManHuanglm"
    const val GITHUB_REPO = "RadioFm"

    /**
     * 更新清单 URL：默认走 GitHub raw 直链（与仓库 update/update.json 同步）。
     * 如需国内加速，可切换为 GitHub Pages / 对象存储（OSS、COS）地址，
     * 仅改此处即可，格式见仓库 update/update.json。
     */
    const val UPDATE_MANIFEST_URL =
        "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/update/update.json"

    /** 版本检查最小间隔（24 小时，毫秒） */
    const val UPDATE_CHECK_MIN_INTERVAL_MS: Long = 24 * 60 * 60 * 1000L

    /** 应用版本更新记录本地缓存上限 */
    const val MAX_CACHED_RELEASES = 30

    // ===== 通知渠道 =====

    const val AUDIO_NOTIFICATION_CHANNEL_ID = "$PACKAGE_NAME.channel.audio"
    const val AUDIO_NOTIFICATION_CHANNEL_NAME = "听电台"

    // ===== 持久化键（DataStore Preferences）=====

    const val KEY_FAVORITES = "favorites"
    const val KEY_PLAY_HISTORY = "play_history"
    const val KEY_THEME_MODE = "theme_mode"
    const val KEY_VOLUME = "volume"
    const val KEY_SLEEP_TIMER = "sleep_timer"
    const val KEY_GRADIENT_WALLPAPER_INDEX = "gradient_wallpaper_index"
    const val KEY_SELECTED_COUNTRY = "selected_country"
    const val KEY_LOCAL_STATIONS = "local_stations"
    const val KEY_PERMISSION_SHOWN = "permission_shown"
    const val KEY_VISUALIZER_ENABLED = "visualizer_enabled"
    const val KEY_VISUALIZER_STYLE = "visualizer_style"
    const val KEY_VISUALIZER_SPEED = "visualizer_speed"

    // 播放行为设置
    const val KEY_AUTO_PLAY_LAST = "auto_play_last"
    const val KEY_MINI_AUTO_COLLAPSE = "mini_auto_collapse"

    // 外观：字体缩放（1.0 标准 / 1.15 大 / 1.3 特大）
    const val KEY_FONT_SCALE = "font_scale"

    // 电台数据更新断点续传
    const val KEY_UPDATE_RESUME_OFFSET = "update_resume_offset"
    const val KEY_UPDATE_RESUME_FETCHED = "update_resume_fetched"
    const val KEY_UPDATE_RESUME_TOTAL = "update_resume_total"

    // 应用更新缓存
    const val KEY_APP_UPDATE_LAST_CHECK = "app_update_last_check_time"
    const val KEY_APP_UPDATE_CACHED_RELEASES = "app_update_cached_releases"
    const val KEY_APP_UPDATE_SKIPPED_VERSION = "app_update_skipped_version"

    // ===== 业务常量 =====

    const val MAX_HISTORY_LENGTH = 10
    const val MAX_FAVORITE_STATIONS = 50
    const val DEFAULT_SLEEP_TIMER_MINUTES = 30
    const val DEFAULT_VOLUME = 0.5f

    /** radio-browser.info API 基础地址 */
    const val RADIO_BROWSER_API_BASE = "https://de1.api.radio-browser.info/json"

    // ===== 网络超时（秒）=====

    const val NETWORK_CONNECT_TIMEOUT = 8L
    const val NETWORK_RECEIVE_TIMEOUT = 20L
    const val NETWORK_SEND_TIMEOUT = 10L

    // ===== 电台数据源参数 =====

    /** 分页加载每页数量 */
    const val PAGE_SIZE = 30

    /** 全量更新批次大小（批次越小，低配机进度条推进越频繁） */
    const val BATCH_SIZE = 500

    /** 批次间让渡主线程时间（毫秒） */
    const val BATCH_DELAY_MS = 100L

    /** 全量缓存默认最大电台数量 */
    const val DEFAULT_MAX_STATIONS = 10000

    /** 支持的音乐分类列表 */
    val supportedCategories = listOf(
        "Pop", "Rock", "Jazz", "Classical", "Electronic", "Hip Hop",
        "Country", "Reggae", "Blues", "R&B", "Latin", "World",
    )
}
