package com.xucong.wordmemory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xucong.wordmemory.data.local.entity.ReviewRecordEntity

@Dao
interface ReviewRecordDao {

    @Insert
    suspend fun insert(record: ReviewRecordEntity): Long

    /** 某天的复习次数（用于连续打卡与今日进度）。 */
    @Query("SELECT COUNT(*) FROM review_records WHERE reviewedAt BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM review_records WHERE wordId = :wordId AND grade >= 2")
    suspend fun correctCountForWord(wordId: Long): Int

    /** 统计页用：取最近一段时间的全部记录，按天聚合放到 Kotlin 里做（量小，简单可测）。 */
    @Query("SELECT reviewedAt, grade FROM review_records WHERE reviewedAt >= :from ORDER BY reviewedAt ASC")
    suspend fun eventsSince(from: Long): List<ReviewEventRow>
}

/** 只取统计需要的两列，顺手也省了一次整行映射。 */
data class ReviewEventRow(val reviewedAt: Long, val grade: Int)
