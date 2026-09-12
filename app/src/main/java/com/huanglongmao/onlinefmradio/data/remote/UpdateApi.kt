package com.huanglongmao.onlinefmradio.data.remote

import com.huanglongmao.onlinefmradio.data.model.UpdateManifestDto
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * 应用版本更新清单接口（方案 2：静态 update.json）。
 * 托管地址可为 GitHub Pages / 对象存储 / 任意静态服务器。
 */
interface UpdateApi {

    @GET
    suspend fun manifest(@Url url: String): UpdateManifestDto
}
