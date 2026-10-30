package com.xucong.wordmemory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一次复习记录，用于统计正确率、连续打卡与后续遗忘曲线调参。 */
@Entity(
    tableName = "review_records",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("wordId"), Index("reviewedAt")],
)
data class ReviewRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val wordId: Long,
    /** 反馈档位：0 = 不认识，1 = 模糊，2 = 认识。 */
    val grade: Int,
    val reviewedAt: Long = System.currentTimeMillis(),
    /** 复习时算出的新间隔，便于回放调度过程。 */
    val intervalDaysAfter: Int = 0,
)
