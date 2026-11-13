package com.xucong.wordmemory.domain.dict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoudaoParserTest {

    /** 真实响应裁出来的最小样本：音标、中文释义、双语例句都在。 */
    private val fullResponse = """
        {
          "ec": {
            "word": [
              {
                "usphone": "rɪˈzɪliənt",
                "ukphone": "rɪˈzɪljənt",
                "return-phrase": "resilient",
                "trs": [
                  { "tr": [ { "l": { "i": ["adj. （人或动物）有复原力的"] } } ] },
                  { "tr": [ { "l": { "i": ["adj. （物质）有弹性的"] } } ] }
                ]
              }
            ]
          },
          "blng_sents_part": {
            "sentence-count": "3",
            "sentence-pair": [
              {
                "sentence": "These emotions can improve our mood, and make us more resilient to stress.",
                "sentence-translation": "这些情绪可以改善心情，使我们在面对压力时更有韧性。"
              },
              {
                "sentence": "Forgotten crops are among the most climate-resilient and nutritious.",
                "sentence-translation": "被遗忘的作物是最具气候韧性和营养的作物之一。"
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `完整响应解析出音标 释义与双语例句`() {
        val entry = YoudaoParser.parse("resilient", fullResponse)

        requireNotNull(entry)
        assertEquals("resilient", entry.term)
        assertEquals("rɪˈzɪliənt", entry.usPhone)
        assertEquals("rɪˈzɪliənt", entry.phonetic)
        assertEquals(2, entry.definitions.size)
        assertEquals("adj. （人或动物）有复原力的", entry.definitions[0])
        assertEquals(2, entry.examples.size)
        assertTrue(entry.examples[0].english.startsWith("These emotions"))
        assertEquals("这些情绪可以改善心情，使我们在面对压力时更有韧性。", entry.examples[0].chinese)
        assertTrue(entry.audioUrl.contains("audio=resilient"))
    }

    @Test
    fun `没有美音时退回英音`() {
        val json = """{"ec":{"word":[{"ukphone":"rɪˈzɪljənt","trs":[{"tr":[{"l":{"i":["adj. 有弹性的"]}}]}]}]}}"""
        val entry = YoudaoParser.parse("resilient", json)

        requireNotNull(entry)
        assertEquals("", entry.usPhone)
        assertEquals("rɪˈzɪljənt", entry.phonetic)
    }

    @Test
    fun `ec 缺失时用 simple 兜底`() {
        val json = """{"simple":{"word":[{"usphone":"ˈsɪmpl","trs":[{"tr":[{"l":{"i":["adj. 简单的"]}}]}]}]}}"""
        val entry = YoudaoParser.parse("simple", json)

        requireNotNull(entry)
        assertEquals("ˈsɪmpl", entry.usPhone)
        assertEquals("adj. 简单的", entry.definitions.single())
    }

    @Test
    fun `例句超过上限只保留前三条`() {
        val pairs = (1..6).joinToString(",") {
            """{"sentence":"sentence $it","sentence-translation":"译文 $it"}"""
        }
        val json = """{"ec":{"word":[{"usphone":"x","trs":[]}]},"blng_sents_part":{"sentence-pair":[$pairs]}}"""

        val entry = YoudaoParser.parse("word", json)

        requireNotNull(entry)
        assertEquals(YoudaoParser.MAX_EXAMPLES, entry.examples.size)
        assertEquals("sentence 1", entry.examples.first().english)
    }

    @Test
    fun `空例句条目被跳过`() {
        val json = """{"blng_sents_part":{"sentence-pair":[
            {"sentence":"","sentence-translation":"空"},
            {"sentence":"real sentence","sentence-translation":"真句子"}]}}"""

        val entry = YoudaoParser.parse("word", json)

        requireNotNull(entry)
        assertEquals(1, entry.examples.size)
        assertEquals("real sentence", entry.examples.single().english)
    }

    @Test
    fun `什么都没有时返回 null`() {
        assertNull(YoudaoParser.parse("word", """{"ec":{"word":[{"trs":[]}]}}"""))
    }

    @Test
    fun `损坏的 JSON 返回 null 而不是抛异常`() {
        assertNull(YoudaoParser.parse("word", """{"ec": """))
        assertNull(YoudaoParser.parse("word", ""))
        assertNull(YoudaoParser.parse("", fullResponse))
    }

    @Test
    fun `音频链接对特殊字符做编码`() {
        val url = YoudaoParser.audioUrlOf("don't stop")
        assertTrue(url.contains("audio=don%27t+stop") || url.contains("audio=don%27t%20stop"))
        assertTrue(url.endsWith("&type=2"))
    }
}
