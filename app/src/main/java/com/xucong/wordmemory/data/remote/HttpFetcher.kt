package com.xucong.wordmemory.data.remote

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 抓取一段文本。抽成接口的唯一目的是让仓库层能在纯 JVM 单测里被替换成假实现，
 * 不需要为了测「缓存命中就不联网」而去起 MockWebServer。
 */
interface HttpFetcher {
    suspend fun get(url: String): String
}

/**
 * 用 OkHttp 而不是手写 HttpURLConnection：
 * 连接池、超时、gzip、重定向、重试这些细节交给成熟库，代码只剩「发请求、看状态码、读 body」。
 */
class OkHttpFetcher(
    private val client: OkHttpClient = DEFAULT_CLIENT,
    private val userAgent: String = DEFAULT_USER_AGENT,
) : HttpFetcher {

    override suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("服务器返回 HTTP ${response.code}")
            }
            response.body.string().orEmpty()
        }
    }

    private companion object {
        const val DEFAULT_USER_AGENT = "WordMemory/1.0 (Android; course-project)"

        val DEFAULT_CLIENT: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
