package com.xucong.wordmemory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 词库：一个词库包含若干单词，例如「CET-4 高频」「B4 结课词表」。 */
@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
