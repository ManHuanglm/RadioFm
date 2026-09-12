package com.huanglongmao.onlinefmradio.ui

/** 导航路由（对应 Flutter 版 navigation_service.dart 的命名路由） */
object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val PLAYER = "player"
    const val SETTINGS = "settings"
    const val STATION_UPDATE = "station_update"
    const val CACHED_STATIONS = "cached_stations"
    const val LOCAL_STATIONS = "local_stations"
    const val RANDOM_STATION = "random_station"
    const val COUNTRY_LIST = "country_list"
    const val LANGUAGE_LIST = "language_list"
    const val TAG_LIST = "tag_list"
    const val RECORDING = "recording"
    const val ALARM = "alarm"
    const val CHANGELOG = "changelog"
    const val HELP = "help"
    const val DEVELOPER = "developer"
    const val LOGS = "logs"

    /** 带参路由模板 */
    const val COUNTRY_STATIONS = "country_stations/{name}/{code}"
    const val LANGUAGE_STATIONS = "language_stations/{name}"
    const val TAG_STATIONS = "tag_stations/{tag}"

    fun countryStations(name: String, code: String) =
        "country_stations/${android.net.Uri.encode(name)}/${android.net.Uri.encode(code)}"
    fun languageStations(name: String) = "language_stations/${android.net.Uri.encode(name)}"
    fun tagStations(tag: String) = "tag_stations/${android.net.Uri.encode(tag)}"

    /** 底部 4 个主 Tab */
    val bottomTabs = listOf(HOME, EXPLORE, FAVORITES, PROFILE)
}
