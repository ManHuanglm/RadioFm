package com.huanglongmao.onlinefmradio.data.remote

import com.huanglongmao.onlinefmradio.data.model.NameCountDto
import com.huanglongmao.onlinefmradio.data.model.RadioStatsDto
import com.huanglongmao.onlinefmradio.data.model.StationDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * radio-browser.info API（对应 Flutter 版 local_station_datasource.dart 的 _request）。
 */
interface RadioBrowserApi {

    /** 电台列表（分页 / 排序 / 隐藏损坏） */
    @GET("stations")
    suspend fun stations(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int = 0,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: String = "true",
        @Query("hidebroken") hidebroken: String? = null,
    ): List<StationDto>

    /** 按 ISO 国家代码精确查询 */
    @GET("stations/bycountrycodeexact/{code}")
    suspend fun byCountryCode(
        @Path("code") code: String,
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: String = "true",
        @Query("hidebroken") hidebroken: String? = null,
    ): List<StationDto>

    /** 按国家名称精确查询 */
    @GET("stations/bycountryexact/{name}")
    suspend fun byCountryExact(
        @Path("name") name: String,
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: String = "true",
        @Query("hidebroken") hidebroken: String? = null,
    ): List<StationDto>

    /** 按标签查询 */
    @GET("stations/bytag/{tag}")
    suspend fun byTag(
        @Path("tag") tag: String,
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: String = "true",
        @Query("hidebroken") hidebroken: String? = null,
    ): List<StationDto>

    /** 按电台名称搜索 */
    @GET("stations/byname/{query}")
    suspend fun byName(
        @Path("query") query: String,
        @Query("limit") limit: Int = 50,
    ): List<StationDto>

    /** 按语言查询 */
    @GET("stations/bylanguage/{language}")
    suspend fun byLanguage(
        @Path("language") language: String,
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: String = "true",
        @Query("hidebroken") hidebroken: String? = null,
    ): List<StationDto>

    /** 按筛选条件查询电台（导出功能用，可组合 countrycode/country/tag/language） */
    @GET("stations")
    suspend fun stationsFiltered(
        @Query("limit") limit: Int,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: String = "true",
        @Query("hidebroken") hidebroken: String = "true",
        @Query("countrycode") countryCode: String? = null,
        @Query("country") country: String? = null,
        @Query("tag") tag: String? = null,
        @Query("language") language: String? = null,
    ): List<StationDto>

    /** 随机电台 */
    @GET("stations/random")
    suspend fun random(
        @Query("limit") limit: Int = 1,
        @Query("hidebroken") hidebroken: String = "true",
    ): List<StationDto>

    /** 平台统计 */
    @GET("stats")
    suspend fun stats(): RadioStatsDto

    /** 国家列表 */
    @GET("countries")
    suspend fun countries(): List<NameCountDto>

    /** 语言列表 */
    @GET("languages")
    suspend fun languages(): List<NameCountDto>

    /** 标签列表 */
    @GET("tags")
    suspend fun tags(
        @Query("limit") limit: Int = 100,
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: String = "true",
    ): List<NameCountDto>
}
