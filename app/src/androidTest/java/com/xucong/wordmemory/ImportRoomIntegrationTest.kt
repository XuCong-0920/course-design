package com.xucong.wordmemory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xucong.wordmemory.data.WordRepository
import com.xucong.wordmemory.data.local.WordDatabase
import com.xucong.wordmemory.domain.importer.WordFileImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 仪器化测试：在真实设备上跑「解析文件 → 事务写入 Room」这一整条链路。
 *
 * 为什么不用 JVM 单测覆盖这段：`@Insert(IGNORE)` 的返回值、唯一索引冲突、
 * `withTransaction` 的原子性都是 SQLite/Room 的运行时行为，只有真跑一遍才算验证过。
 * 用内存库（inMemoryDatabaseBuilder），不会污染 App 自己的 word_memory.db。
 *
 * 运行：`./gradlew :app:connectedDebugAndroidTest`
 */
@RunWith(AndroidJUnit4::class)
class ImportRoomIntegrationTest {

    private lateinit var database: WordDatabase
    private lateinit var repository: WordRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WordDatabase::class.java).build()
        repository = WordRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun csvImport_writesDeckAndWords_skippingBadRows() = runBlocking {
        val csv = """
            term,phonetic,meaning,example
            resilient,/rɪˈzɪliənt/,adj. 有韧性的,She stayed resilient through the setbacks.
            ,缺少单词的行会被跳过,
            meticulous,/məˈtɪkjələs/,adj. 一丝不苟的,He kept meticulous notes.
        """.trimIndent()

        val preview = WordFileImporter.parse("demo.csv", csv)
        assertEquals(2, preview.words.size)
        assertEquals(1, preview.problems.size)

        val outcome = repository.importDeck(preview.deckName, "由文件导入", preview.words)

        assertTrue(outcome.imported)
        assertEquals(2, outcome.inserted)
        assertEquals(0, outcome.skipped)
        assertEquals(2, repository.wordCount(outcome.deckId))

        val words = repository.wordsOf(outcome.deckId)
        assertEquals(listOf("meticulous", "resilient"), words.map { it.term })

        val resilient = words.first { it.term == "resilient" }
        assertEquals("/rɪˈzɪliənt/", resilient.phonetic)
        assertEquals("adj. 有韧性的", resilient.meaning)
        // 新导入的词必须是「现在就该复习」，SRS 进度从零开始
        assertEquals(0, resilient.repetition)
        assertEquals(2.5, resilient.easeFactor, 0.0001)
        assertTrue(resilient.dueAt <= System.currentTimeMillis())
    }

    @Test
    fun jsonImport_usesNameFromFileContent() = runBlocking {
        val json = """
            { "name": "雅思核心", "words": [
              { "term": "ubiquitous", "meaning": "adj. 无处不在的" },
              { "term": "candid", "meaning": "adj. 坦率的" }
            ] }
        """.trimIndent()

        val preview = WordFileImporter.parse("ignored.json", json)
        assertEquals("雅思核心", preview.deckName)

        val outcome = repository.importDeck(preview.deckName, "由文件导入", preview.words)

        assertEquals(2, outcome.inserted)
        val decks = repository.observeDeckSummaries().first()
        assertEquals(1, decks.size)
        assertEquals("雅思核心", decks[0].name)
        assertEquals(2, decks[0].wordCount)
    }

    @Test
    fun importingEmptyList_writesNothing() = runBlocking {
        val outcome = repository.importDeck("空词库", "", emptyList())

        assertFalse(outcome.imported)
        assertEquals(WordRepository.NOT_IMPORTED, outcome.deckId)
        assertEquals(0, repository.totalWordCount())
        assertTrue(repository.observeDeckSummaries().first().isEmpty())
    }
}
