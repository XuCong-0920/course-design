package com.xucong.wordmemory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xucong.wordmemory.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM words WHERE deckId = :deckId ORDER BY term COLLATE NOCASE ASC")
    fun observeByDeck(deckId: Long): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE id = :wordId")
    fun observeById(wordId: Long): Flow<WordEntity?>

    @Query("SELECT * FROM words WHERE deckId = :deckId AND dueAt <= :now ORDER BY dueAt ASC")
    suspend fun dueWords(deckId: Long, now: Long = System.currentTimeMillis()): List<WordEntity>

    /**
     * 跨词库的到期队列：复习页不区分词库，按到期时间从早到晚取前 [limit] 个。
     * 加 limit 是为了避免首次学习时一次性把几百个新词全塞进内存。
     */
    @Query("SELECT * FROM words WHERE dueAt <= :now ORDER BY dueAt ASC LIMIT :limit")
    suspend fun dueWordsAll(now: Long, limit: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE id = :wordId")
    suspend fun getById(wordId: Long): WordEntity?

    @Query("SELECT COUNT(*) FROM words WHERE deckId = :deckId")
    suspend fun countByDeck(deckId: Long): Int

    /** 全库到期数量，供每日提醒判断「今天到底有没有东西可复习」。 */
    @Query("SELECT COUNT(*) FROM words WHERE dueAt <= :now")
    suspend fun countDue(now: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>): List<Long>

    @Update
    suspend fun update(word: WordEntity)

    @Query("DELETE FROM words WHERE id = :wordId")
    suspend fun deleteById(wordId: Long)
}
