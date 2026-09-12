package com.huanglongmao.onlinefmradio.core.network

import com.huanglongmao.onlinefmradio.core.constants.AppConstants
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络层工厂（对应 Flutter 版 Dio 配置）：
 * - 超时：连接 8s / 接收 20s / 发送 10s
 * - 失败自动重试 1 次，对抗直播弱网抖动
 */
object RetrofitFactory {

    /** JSON 配置：忽略未知字段，容错空类型 */
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = false
    }

    @PublishedApi
    internal val baseClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(AppConstants.NETWORK_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(AppConstants.NETWORK_RECEIVE_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(AppConstants.NETWORK_SEND_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // 电台请求通用头（对齐 Dio 配置）
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(RetryInterceptor(1))
            .addInterceptor(logging)
            .build()
    }

    /** 创建面向指定 baseUrl 的 Retrofit 服务 */
    inline fun <reified T> create(baseUrl: String): T =
        Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(baseClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(T::class.java)

    /**
     * 简单重试拦截器：连接失败时按次数重试（仅对幂等 GET 有意义，
     * 本项目全部为 GET 请求）。
     */
    private class RetryInterceptor(private val maxRetry: Int) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var lastException: java.io.IOException? = null
            repeat(maxRetry + 1) { attempt ->
                try {
                    val request = chain.request()
                    val response = chain.proceed(request)
                    if (response.isSuccessful || attempt == maxRetry) return response
                    response.close()
                } catch (e: java.io.IOException) {
                    lastException = e
                    if (attempt == maxRetry) throw e
                }
            }
            throw lastException ?: java.io.IOException("network request failed")
        }
    }
}
