package com.xucong.wordmemory.domain.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 导入解析的单元测试。重点不是「正常文件能读」，而是**坏文件不会整份失败**：
 * 少一列、单词为空、JSON 少个引号，都只能让那一行进 problems，其余照常导入。
 */
class WordFileImporterTest {

    @Test
    fun `CSV 带表头四列完整解析`() {
        val csv = """
            term,phonetic,meaning,example
            abandon,/əˈbændən/,v. 放弃；抛弃,He abandoned the plan.
            keen,/kiːn/,adj. 热衷的；敏锐的,She is keen on music.
        """.trimIndent()

        val preview = WordFileImporter.parse("B4词汇.csv", csv)

        assertEquals("B4词汇", preview.deckName)
        assertEquals(2, preview.words.size)
        assertEquals(0, preview.problems.size)
        assertEquals("abandon", preview.words[0].term)
        assertEquals("/əˈbændən/", preview.words[0].phonetic)
        assertEquals("v. 放弃；抛弃", preview.words[0].meaning)
        assertEquals("He abandoned the plan.", preview.words[0].example)
    }

    @Test
    fun `CSV 只有两列时第二列当释义`() {
        val preview = WordFileImporter.parse("min.csv", "abandon,放弃\nkeen,热衷的")

        assertEquals(2, preview.words.size)
        assertEquals("", preview.words[0].phonetic)
        assertEquals("放弃", preview.words[0].meaning)
        assertEquals("", preview.words[0].example)
    }

    @Test
    fun `CSV 带引号与字段内逗号`() {
        val csv = "abandon,/əˈbændən/,\"v. 放弃, 抛弃\",\"He said \"\"no\"\" and left.\""

        val preview = WordFileImporter.parse("q.csv", csv)

        assertEquals(1, preview.words.size)
        assertEquals("v. 放弃, 抛弃", preview.words[0].meaning)
        assertEquals("He said \"no\" and left.", preview.words[0].example)
    }

    @Test
    fun `CSV 内部重复词只保留第一条并计数`() {
        val csv = "Abandon,放弃\nabandon,抛弃\nkeen,热衷的"

        val preview = WordFileImporter.parse("dup.csv", csv)

        assertEquals(2, preview.words.size)
        assertEquals(1, preview.duplicateCount)
        assertEquals("放弃", preview.words[0].meaning)
        assertEquals(3, preview.total)
    }

    @Test
    fun `CSV 坏行只跳过该行并给出行号`() {
        val csv = "abandon,放弃\n,没有单词\nkeen,\nbenefit,好处"

        val preview = WordFileImporter.parse("bad.csv", csv)

        assertEquals(2, preview.words.size)
        assertEquals(2, preview.problems.size)
        assertEquals(2, preview.problems[0].line)
        assertEquals("单词为空", preview.problems[0].reason)
        assertEquals(3, preview.problems[1].line)
        assertEquals("释义为空", preview.problems[1].reason)
    }

    @Test
    fun `单词过长会被拒绝`() {
        val longTerm = "a".repeat(WordFileImporter.MAX_TERM_LENGTH + 1)

        val preview = WordFileImporter.parse("long.csv", "$longTerm,释义")

        assertEquals(0, preview.words.size)
        assertEquals(1, preview.problems.size)
        assertTrue(preview.problems[0].reason.contains("超过"))
    }

    @Test
    fun `JSON 顶层数组按对象顺序解析`() {
        val json = """
            [
              { "term": "abandon", "phonetic": "/əˈbændən/", "meaning": "v. 放弃" },
              { "term": "keen", "meaning": "adj. 热衷的", "example": "keen on music" }
            ]
        """.trimIndent()

        val preview = WordFileImporter.parse("words.json", json)

        assertEquals("words", preview.deckName)
        assertEquals(2, preview.words.size)
        assertEquals("v. 放弃", preview.words[0].meaning)
        assertEquals("keen on music", preview.words[1].example)
    }

    @Test
    fun `JSON 对象形态用 name 作为词库名`() {
        val json = """
            { "name": "四级高频", "description": "示例", "words": [ { "term": "benefit", "meaning": "n. 好处" } ] }
        """.trimIndent()

        val preview = WordFileImporter.parse("whatever.json", json)

        assertEquals("四级高频", preview.deckName)
        assertEquals(1, preview.words.size)
        assertEquals("benefit", preview.words[0].term)
    }

    @Test
    fun `JSON 缺字段只记该对象的问题`() {
        val json = """[ { "term": "abandon" }, { "term": "keen", "meaning": "热衷的" } ]"""

        val preview = WordFileImporter.parse("p.json", json)

        assertEquals(1, preview.words.size)
        assertEquals(1, preview.problems.size)
        assertEquals(1, preview.problems[0].line)
        assertEquals("释义为空", preview.problems[0].reason)
    }

    @Test
    fun `JSON 格式错误不抛异常 返回一条可读原因`() {
        val broken = """[ { "term": "abandon", "meaning": "放弃 } ]"""

        val preview = WordFileImporter.parse("b.json", broken)

        assertEquals(0, preview.words.size)
        assertEquals(1, preview.problems.size)
        assertTrue(preview.problems[0].reason.isNotBlank())
    }

    @Test
    fun `文件名空白时用默认词库名`() {
        val preview = WordFileImporter.parse(".csv", "abandon,放弃")

        assertEquals("导入的词库", preview.deckName)
        assertEquals(1, preview.words.size)
    }

    @Test
    fun `空文件得到空结果而不是异常`() {
        val preview = WordFileImporter.parse("empty.csv", "")

        assertTrue(preview.isEmpty)
        assertEquals(0, preview.total)
    }
}
