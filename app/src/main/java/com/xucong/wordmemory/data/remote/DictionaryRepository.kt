package com.xucong.wordmemory.data.remote

import com.xucong.wordmemory.domain.dict.DictEntry
import com.xucong.wordmemory.domain.dict.YoudaoParser
import java.io.IOException
import java.net.URLEncoder

/**
 * 在线词典仓库：缓存优先 → 联网 → 解析 → 回写缓存。
 *
 * 失败一律转成 [Result.failure]，由 UI 决定是提示「网络不可用」还是「没查到」——
 * 页面不该因为一次 404 或断网而崩溃或卡死。
 */
class DictionaryRepository(
    private val cache: DictCache,
    private val fetcher: HttpFetcher = OkHttpFetcher(),
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun lookup(term: String, forceRefresh: Boolean = false): Result<DictEntry> {
        val normalized = term.trim()
        if (normalized.isEmpty()) {
            return Result.failure(IllegalArgumentException("单词不能为空"))
        }
        if (!forceRefresh) {
            cache.get(normalized)?.let { return Result.success(it) }
        }
        return try {
            val json = fetcher.get(endpoint(normalized))
            val entry = YoudaoParser.parse(normalized, json)
                ?: return Result.failure(IOException("没有查到「$normalized」的释义"))
            cache.put(entry, clock())
            Result.success(entry)
        } catch (io: IOException) {
            Result.failure(io)
        } catch (other: RuntimeException) {
            // URL 编码、连接串构造等运行时异常同样按「联网失败」处理
            Result.failure(IOException("联网失败：${other.message}", other))
        }
    }

    /** 发音直链：不需要先查词，MediaPlayer 直接流式播放。 */
    fun audioUrl(term: String, american: Boolean = true): String =
        YoudaoParser.audioUrlOf(term, type = if (american) 2 else 1)

    companion object {
        private const val BASE = "https://dict.youdao.com/jsonapi"

        /** dicts 参数要求带上，否则响应里可能没有双语例句段落。 */
        fun endpoint(term: String): String {
            val encoded = URLEncoder.encode(term.trim(), "UTF-8")
            val dicts = URLEncoder.encode("{\"count\":99}", "UTF-8")
            return "$BASE?q=$encoded&dicts=$dicts"
        }
    }
}
