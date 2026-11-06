package com.xucong.wordmemory.domain.quiz

import kotlin.random.Random

/** 出题需要的最小单词投影，同样刻意不依赖 Room 实体。 */
data class QuizWord(
    val id: Long,
    val term: String,
    val phonetic: String,
    val meaning: String,
    val example: String,
)

/** 一道选择题：题干是单词，[options] 是若干中文释义，[correctIndex] 指向正确项。 */
data class QuizQuestion(
    val word: QuizWord,
    val options: List<String>,
    val correctIndex: Int,
)

enum class QuizMode { CHOICE, SPELLING }

/**
 * 测试模式的出题逻辑：抽词、造干扰项、判拼写。
 *
 * 抽词与打乱都走注入的 [Random]，测试里传固定种子就能写成确定的断言；
 * 这里也不碰数据库 —— 测试模式**不写复习记录**，避免用户随手一测就把遗忘曲线带偏。
 */
object QuizBuilder {

    const val DEFAULT_COUNT = 10
    private const val OPTION_COUNT = 4

    fun build(
        words: List<QuizWord>,
        count: Int = DEFAULT_COUNT,
        random: Random = Random.Default,
    ): List<QuizQuestion> {
        if (words.isEmpty() || count <= 0) return emptyList()

        val picked = words.shuffled(random).take(count)
        val allMeanings = words.map { it.meaning }.distinct()

        return picked.map { word ->
            val distractors = allMeanings
                .filter { it != word.meaning }
                .shuffled(random)
                .take(OPTION_COUNT - 1)
            val options = (distractors + word.meaning).shuffled(random)
            QuizQuestion(word = word, options = options, correctIndex = options.indexOf(word.meaning))
        }
    }

    /** 拼写判定：忽略大小写与首尾空格；内部空格保留（短语也要能拼）。 */
    fun isSpellingCorrect(input: String, term: String): Boolean =
        input.trim().equals(term.trim(), ignoreCase = true)
}
