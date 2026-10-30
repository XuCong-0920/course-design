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

    @Query("SELECT COUNT(*) FROM words WHERE deckId = :deckId")
    suspend fun countByDeck(deckId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordEntity>): List<Long>

    @Update
    suspend fun update(word: WordEntity)

    @Query("DELETE FROM words WHERE id = :wordId")
    suspend fun deleteById(wordId: Long)
}
