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
}
