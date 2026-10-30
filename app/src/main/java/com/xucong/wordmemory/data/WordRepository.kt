package com.xucong.wordmemory.data

import com.xucong.wordmemory.data.local.WordDatabase
import com.xucong.wordmemory.data.local.dao.DeckSummary
import com.xucong.wordmemory.data.local.entity.DeckEntity
import com.xucong.wordmemory.data.local.entity.WordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

/**
 * 唯一的数据入口：ViewModel 只依赖它，不直接碰 DAO / Room，
 * 后续加缓存、加网络同步时替换实现即可。
 */
class WordRepository(private val database: WordDatabase) {

    private val deckDao = database.deckDao()
    private val wordDao = database.wordDao()
    private val reviewRecordDao = database.reviewRecordDao()

    fun observeDeckSummaries(now: Long = System.currentTimeMillis()): Flow<List<DeckSummary>> =
        deckDao.observeSummaries(now)

    fun observeDeck(deckId: Long): Flow<DeckEntity?> = deckDao.observeById(deckId)

    fun observeWords(deckId: Long): Flow<List<WordEntity>> = wordDao.observeByDeck(deckId)

    fun observeWord(wordId: Long): Flow<WordEntity?> = wordDao.observeById(wordId)

    suspend fun dueWords(deckId: Long, now: Long = System.currentTimeMillis()): List<WordEntity> =
        wordDao.dueWords(deckId, now)

    suspend fun wordCount(deckId: Long): Int = wordDao.countByDeck(deckId)

    suspend fun addWord(word: WordEntity): Long = wordDao.insert(word)

    suspend fun deleteWord(wordId: Long) = wordDao.deleteById(wordId)

    /**
     * 首次启动时写入内置示例词库。用事务保证「词库 + 其下的单词」要么全成功、要么全失败，
     * 避免出现空词库或半截数据。
     */
    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (deckDao.count() > 0) return@withContext

        database.withTransaction {
            SeedData.decks.forEach { seedDeck ->
                val deckId = deckDao.insert(
                    DeckEntity(name = seedDeck.name, description = seedDeck.description),
                )
                val now = System.currentTimeMillis()
                wordDao.insertAll(
                    seedDeck.words.map { seedWord ->
                        WordEntity(
                            deckId = deckId,
                            term = seedWord.term,
                            phonetic = seedWord.phonetic,
                            meaning = seedWord.meaning,
                            example = seedWord.example,
                            dueAt = now,
                        )
                    },
                )
            }
        }
    }
}
