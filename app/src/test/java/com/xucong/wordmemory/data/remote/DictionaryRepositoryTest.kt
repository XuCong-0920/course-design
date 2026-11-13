package com.xucong.wordmemory.data.remote

import com.xucong.wordmemory.domain.dict.DictEntry
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryRepositoryTest {

    private val sampleJson = """
        {"ec":{"word":[{"usphone":"rɪˈzɪliənt","trs":[{"tr":[{"l":{"i":["adj. 有复原力的"]}}]}]}]},
         "blng_sents_part":{"sentence-pair":[{"sentence":"Stay resilient.","sentence-translation":"保持韧性。"}]}}
    """.trimIndent()

    private class FakeFetcher(
        private val json: String,
        private val failure: IOException? = null,
    ) : HttpFetcher {
        var calls = 0
            private set
        val urls = mutableListOf<String>()

        override suspend fun get(url: String): String {
            calls++
            urls += url
            failure?.let { throw it }
            return json
        }
    }

    private class MemoryCache : DictCache {
        val entries = mutableMapOf<String, DictEntry>()
        var puts = 0
            private set

        override fun get(term: String): DictEntry? = entries[term]

        override fun put(entry: DictEntry, savedAt: Long) {
            puts++
            entries[entry.term] = entry
        }
    }

    @Test
    fun `首次查询联网并把结果写进缓存`() = runBlocking {
        val fetcher = FakeFetcher(sampleJson)
        val cache = MemoryCache()
        val repository = DictionaryRepository(cache = cache, fetcher = fetcher) { 1_000L }

        val result = repository.lookup("resilient")

        assertTrue(result.isSuccess)
        assertEquals("有复原力的", result.getOrThrow().definitions.single().removePrefix("adj. "))
        assertEquals(1, fetcher.calls)
        assertEquals(1, cache.puts)
        assertTrue(fetcher.urls.single().contains("q=resilient"))
    }

    @Test
    fun `缓存命中时不再联网`() = runBlocking {
        val cache = MemoryCache().apply {
            entries["resilient"] = DictEntry(term = "resilient", definitions = listOf("adj. 有弹性的"))
        }
        val fetcher = FakeFetcher(sampleJson)

        val result = DictionaryRepository(cache = cache, fetcher = fetcher).lookup("resilient")

        assertEquals("adj. 有弹性的", result.getOrThrow().definitions.single())
        assertEquals(0, fetcher.calls)
    }

    @Test
    fun `强制刷新绕过缓存`() = runBlocking {
        val cache = MemoryCache().apply {
            entries["resilient"] = DictEntry(term = "resilient", definitions = listOf("旧释义"))
        }
        val fetcher = FakeFetcher(sampleJson)

        val result = DictionaryRepository(cache = cache, fetcher = fetcher)
            .lookup("resilient", forceRefresh = true)

        assertEquals(1, fetcher.calls)
        assertTrue(result.getOrThrow().definitions.single().startsWith("adj."))
    }

    @Test
    fun `联网失败返回 failure 且不写缓存`() = runBlocking {
        val cache = MemoryCache()
        val fetcher = FakeFetcher(sampleJson, failure = IOException("连接超时"))

        val result = DictionaryRepository(cache = cache, fetcher = fetcher).lookup("resilient")

        assertTrue(result.isFailure)
        assertEquals("连接超时", result.exceptionOrNull()?.message)
        assertEquals(0, cache.puts)
        assertTrue(cache.entries.isEmpty())
    }

    @Test
    fun `查不到释义时返回 failure`() = runBlocking {
        val cache = MemoryCache()
        val fetcher = FakeFetcher("""{"ec":{"word":[{"trs":[]}]}}""")

        val result = DictionaryRepository(cache = cache, fetcher = fetcher).lookup("zzzz")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("没有查到"))
        assertEquals(0, cache.puts)
    }

    @Test
    fun `空单词直接失败 不发起请求`() = runBlocking {
        val fetcher = FakeFetcher(sampleJson)

        val result = DictionaryRepository(cache = MemoryCache(), fetcher = fetcher).lookup("   ")

        assertTrue(result.isFailure)
        assertEquals(0, fetcher.calls)
    }

    @Test
    fun `发音链接走美音 也可以切成英音`() {
        val repository = DictionaryRepository(cache = MemoryCache(), fetcher = FakeFetcher(sampleJson))

        assertTrue(repository.audioUrl("resilient").endsWith("type=2"))
        assertTrue(repository.audioUrl("resilient", american = false).endsWith("type=1"))
    }
}
