package com.xucong.wordmemory

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xucong.wordmemory.data.remote.DictionaryRepository
import com.xucong.wordmemory.data.remote.HttpFetcher
import com.xucong.wordmemory.data.remote.OkHttpFetcher
import com.xucong.wordmemory.data.remote.PrefsDictCache
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 真机上的联网链路测试：OkHttp 请求有道 → Gson 解析 → 写入 SharedPreferences 缓存。
 *
 * 纯 JVM 单测用的是 FakeFetcher，证明不了「真实接口 + 真机网络」这一环，
 * 所以这里用真实 OkHttpFetcher 打一次线上接口，再验证第二次走的是缓存。
 */
@RunWith(AndroidJUnit4::class)
class DictionaryLiveTest {

    private lateinit var repository: DictionaryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        PrefsDictCache(context).clear()
        repository = DictionaryRepository(cache = PrefsDictCache(context), fetcher = OkHttpFetcher())
    }

    @Test
    fun 线上查词能拿到释义与双语例句() = runBlocking {
        val result = repository.lookup("resilient")

        assertTrue("查询失败：${result.exceptionOrNull()}", result.isSuccess)
        val entry = result.getOrThrow()
        assertEquals("resilient", entry.term)
        assertTrue("音标为空", entry.phonetic.isNotBlank())
        assertTrue("释义为空", entry.definitions.isNotEmpty())
        assertTrue("例句为空", entry.examples.isNotEmpty())
        assertTrue("例句没有中文", entry.examples.first().chinese.isNotBlank())
        assertTrue("音频地址不对", entry.audioUrl.contains("audio=resilient"))
    }

    @Test
    fun 第二次查询命中缓存不再联网() = runBlocking {
        val first = repository.lookup("serendipity")
        assertTrue("首次查询失败：${first.exceptionOrNull()}", first.isSuccess)

        // 把 fetcher 换成必定失败的实现：还能拿到结果，就说明走的是缓存
        val offline = DictionaryRepository(
            cache = PrefsDictCache(ApplicationProvider.getApplicationContext()),
            fetcher = object : HttpFetcher {
                override suspend fun get(url: String): String = error("不应该联网：$url")
            },
        )
        val second = offline.lookup("serendipity")

        assertTrue("缓存未命中：${second.exceptionOrNull()}", second.isSuccess)
        assertEquals(first.getOrThrow().definitions, second.getOrThrow().definitions)
    }

    @Test
    fun 发音直链返回的是可播放的音频() = runBlocking {
        val url = repository.audioUrl("resilient", american = true)
        val bytes = URL(url).openStream().use { it.readBytes() }

        assertTrue("音频太小：${bytes.size}", bytes.size > 2_000)
        // 有道的 mp3 前面带 ID3v2 标签（"ID3"），也接受裸帧（0xFF 0xEx 开头的同步字）
        val hasId3Tag = bytes[0] == 'I'.code.toByte() &&
            bytes[1] == 'D'.code.toByte() &&
            bytes[2] == '3'.code.toByte()
        val hasFrameSync = (bytes[0].toInt() and 0xFF) == 0xFF &&
            (bytes[1].toInt() and 0xE0) == 0xE0
        assertTrue("不是 mp3 数据", hasId3Tag || hasFrameSync)
    }
}
