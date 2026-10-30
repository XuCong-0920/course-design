package com.xucong.wordmemory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词。除词条内容外，还内嵌了 SRS 调度状态（repetition / intervalDays / easeFactor / dueAt），
 * 这样复习队列只需一条 `WHERE dueAt <= now` 的查询，避免每张卡片再关联一张状态表。
 */
@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("deckId"),
        Index(value = ["deckId", "term"], unique = true),
    ],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckId: Long,
    val term: String,
    val meaning: String,
    val phonetic: String = "",
    val example: String = "",
    /** 连续答对次数（SM-2 的 repetition）。 */
    val repetition: Int = 0,
    /** 当前间隔天数；0 表示新词、尚未安排。 */
    val intervalDays: Int = 0,
    /** 难度因子（SM-2 的 easiness factor），初始 2.5。 */
    val easeFactor: Double = 2.5,
    /** 下次到期时间（毫秒时间戳）；新词默认立即可复习。 */
    val dueAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
