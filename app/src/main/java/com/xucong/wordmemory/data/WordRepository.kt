package com.xucong.wordmemory.data

import com.xucong.wordmemory.data.local.WordDatabase
import com.xucong.wordmemory.data.local.dao.DeckSummary
import com.xucong.wordmemory.data.local.entity.DeckEntity
import com.xucong.wordmemory.data.local.entity.ReviewRecordEntity
import com.xucong.wordmemory.data.local.entity.WordEntity
import com.xucong.wordmemory.domain.srs.ReviewGrade
import com.xucong.wordmemory.domain.srs.SrsScheduler
import com.xucong.wordmemory.domain.srs.SrsState
import com.xucong.wordmemory.domain.stats.ReviewEvent
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

    suspend fun wordsOf(deckId: Long): List<WordEntity> = wordDao.listByDeck(deckId)

    fun observeWord(wordId: Long): Flow<WordEntity?> = wordDao.observeById(wordId)

    suspend fun dueWords(deckId: Long, now: Long = System.currentTimeMillis()): List<WordEntity> =
        wordDao.dueWords(deckId, now)

    /** 复习页的跨词库到期队列。 */
    suspend fun dueQueue(now: Long = System.currentTimeMillis(), limit: Int = DEFAULT_QUEUE_LIMIT): List<WordEntity> =
        wordDao.dueWordsAll(now, limit)

    /**
     * 提交一次复习反馈：先算新调度状态，再在**同一个事务**里更新单词与写入复习记录。
     * 两者必须同生共死——否则会出现「记录写了但间隔没涨」或反之，统计与调度就对不上了。
     */
    suspend fun submitReview(
        word: WordEntity,
        grade: ReviewGrade,
        now: Long = System.currentTimeMillis(),
    ): SrsState {
        val before = SrsState(
            repetition = word.repetition,
            intervalDays = word.intervalDays,
            easeFactor = word.easeFactor,
            dueAt = word.dueAt,
        )
        val after = SrsScheduler.schedule(before, grade, now)

        database.withTransaction {
            wordDao.update(
                word.copy(
                    repetition = after.repetition,
                    intervalDays = after.intervalDays,
                    easeFactor = after.easeFactor,
                    dueAt = after.dueAt,
                    lastReviewedAt = now,
                ),
            )
            reviewRecordDao.insert(
                ReviewRecordEntity(
                    wordId = word.id,
                    grade = grade.toDbValue(),
                    reviewedAt = now,
                    intervalDaysAfter = after.intervalDays,
                ),
            )
        }
        return after
    }

    suspend fun wordCount(deckId: Long): Int = wordDao.countByDeck(deckId)

    suspend fun totalWordCount(): Int = wordDao.countAll()

    suspend fun dueNow(now: Long = System.currentTimeMillis()): Int = wordDao.countDue(now)

    /** 统计页的数据源：最近 [since] 之后的复习事件（只投影两列，不整行读取）。 */
    suspend fun reviewEventsSince(since: Long): List<ReviewEvent> =
        reviewRecordDao.eventsSince(since).map { ReviewEvent(it.reviewedAt, it.grade) }

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

    companion object {
        /** 单次复习最多取多少个词，防止新词库首次学习时一次加载过多。 */
        private const val DEFAULT_QUEUE_LIMIT = 50
    }
}
