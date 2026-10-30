package com.xucong.wordmemory.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xucong.wordmemory.data.local.dao.DeckDao
import com.xucong.wordmemory.data.local.dao.ReviewRecordDao
import com.xucong.wordmemory.data.local.dao.WordDao
import com.xucong.wordmemory.data.local.entity.DeckEntity
import com.xucong.wordmemory.data.local.entity.ReviewRecordEntity
import com.xucong.wordmemory.data.local.entity.WordEntity

/**
 * 本地数据库。单例 + 双检锁；后续引入迁移时在 builder 里 addMigrations。
 * exportSchema = false：骨架阶段不导出 schema 文件，等表结构稳定后再打开。
 */
@Database(
    entities = [DeckEntity::class, WordEntity::class, ReviewRecordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WordDatabase : RoomDatabase() {

    abstract fun deckDao(): DeckDao

    abstract fun wordDao(): WordDao

    abstract fun reviewRecordDao(): ReviewRecordDao

    companion object {
        private const val DB_NAME = "word_memory.db"

        @Volatile
        private var instance: WordDatabase? = null

        fun get(context: Context): WordDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WordDatabase::class.java,
                    DB_NAME,
                ).build().also { instance = it }
            }
    }
}
