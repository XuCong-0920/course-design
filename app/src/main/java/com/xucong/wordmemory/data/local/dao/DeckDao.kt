package com.xucong.wordmemory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xucong.wordmemory.data.local.entity.DeckEntity
import kotlinx.coroutines.flow.Flow

/** 词库列表用的聚合行：词库本身 + 单词数 + 今日到期数。 */
data class DeckSummary(
    val id: Long,
    val name: String,
    val description: String,
    val wordCount: Int,
    val dueCount: Int,
)

@Dao
interface DeckDao {

    @Query(
        """
        SELECT d.id AS id,
               d.name AS name,
               d.description AS description,
               (SELECT COUNT(*) FROM words w WHERE w.deckId = d.id) AS wordCount,
               (SELECT COUNT(*) FROM words w WHERE w.deckId = d.id AND w.dueAt <= :now) AS dueCount
        FROM decks d
        ORDER BY d.createdAt ASC
        """,
    )
    fun observeSummaries(now: Long = System.currentTimeMillis()): Flow<List<DeckSummary>>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    fun observeById(deckId: Long): Flow<DeckEntity?>

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(deck: DeckEntity): Long

    @Insert
    suspend fun insertAll(decks: List<DeckEntity>): List<Long>

    @Update
    suspend fun update(deck: DeckEntity)

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteById(deckId: Long)
}
