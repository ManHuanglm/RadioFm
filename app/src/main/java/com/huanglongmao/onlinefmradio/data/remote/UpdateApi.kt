package com.huanglongmao.onlinefmradio.data.remote

import com.huanglongmao.onlinefmradio.data.model.GitHubReleaseDto
import com.huanglongmao.onlinefmradio.data.model.UpdateManifestDto
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * 应用版本更新接口：
 * - [githubReleases]：GitHub Releases API（首选，发布即自动可检测）
 * - [manifest]：静态 update.json 清单（回退，托管于 GitHub Pages / OSS）
 */
interface UpdateApi {

    @GET
    suspend fun githubReleases(@Url url: String): List<GitHubReleaseDto>

    @GET
    suspend fun manifest(@Url url: String): UpdateManifestDto
}
