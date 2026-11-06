package com.xucong.wordmemory.domain.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** QuizBuilder 的纯 JVM 单元测试：固定随机种子，断言可复现。 */
class QuizBuilderTest {

    private val words = listOf(
        QuizWord(1, "abandon", "/əˈbændən/", "v. 放弃；抛弃", "He abandoned the plan."),
        QuizWord(2, "benefit", "/ˈbenɪfɪt/", "n. 益处 v. 有益于", "It benefits everyone."),
        QuizWord(3, "crucial", "/ˈkruːʃl/", "adj. 至关重要的", "Timing is crucial."),
        QuizWord(4, "decline", "/dɪˈklaɪn/", "v. 下降；婉拒", "Sales declined."),
        QuizWord(5, "efficient", "/ɪˈfɪʃnt/", "adj. 高效的", "An efficient method."),
    )

    private fun random() = Random(42)

    @Test
    fun `题数不超过设置值 也不超过词库大小`() {
        assertEquals(3, QuizBuilder.build(words, count = 3, random = random()).size)
        assertEquals(5, QuizBuilder.build(words, count = 10, random = random()).size)
        assertTrue(QuizBuilder.build(emptyList(), count = 10, random = random()).isEmpty())
    }

    @Test
    fun `每题四个选项 且正确项恰好出现一次`() {
        val questions = QuizBuilder.build(words, count = 5, random = random())

        questions.forEach { question ->
            assertEquals(4, question.options.size)
            assertEquals(1, question.options.count { it == question.word.meaning })
            assertEquals(question.word.meaning, question.options[question.correctIndex])
        }
    }

    @Test
    fun `选项之间互不重复`() {
        QuizBuilder.build(words, count = 5, random = random()).forEach { question ->
            assertEquals(question.options.size, question.options.distinct().size)
        }
    }

    @Test
    fun `同一个种子出同一套题 换种子结果不同`() {
        val a = QuizBuilder.build(words, count = 5, random = Random(7))
        val b = QuizBuilder.build(words, count = 5, random = Random(7))
        val c = QuizBuilder.build(words, count = 5, random = Random(8))

        assertEquals(a.map { it.word.id }, b.map { it.word.id })
        assertFalse(a.zip(c).all { (x, y) -> x.word.id == y.word.id })
    }

    @Test
    fun `词库不足四个词时 选项数量跟着变少 不会塞空字符串`() {
        val two = words.take(2)

        val questions = QuizBuilder.build(two, count = 2, random = random())

        questions.forEach { question ->
            assertEquals(2, question.options.size)
            assertTrue(question.options.none { it.isBlank() })
            assertEquals(question.word.meaning, question.options[question.correctIndex])
        }
    }

    @Test
    fun `拼写判定忽略大小写与首尾空格`() {
        assertTrue(QuizBuilder.isSpellingCorrect("abandon", "abandon"))
        assertTrue(QuizBuilder.isSpellingCorrect("  Abandon ", "abandon"))
        assertTrue(QuizBuilder.isSpellingCorrect("ABANDON", "abandon"))
        assertFalse(QuizBuilder.isSpellingCorrect("abandan", "abandon"))
        assertFalse(QuizBuilder.isSpellingCorrect("", "abandon"))
    }
}
