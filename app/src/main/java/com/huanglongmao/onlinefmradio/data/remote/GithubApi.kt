package com.huanglongmao.onlinefmradio.data.remote

import com.huanglongmao.onlinefmradio.data.model.GithubReleaseDto
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * GitHub Releases API（用于应用版本更新检查）。
 */
interface GithubApi {

    @GET
    suspend fun releases(@Url url: String): List<GithubReleaseDto>
}
