package com.xucong.wordmemory.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.xucong.wordmemory.data.local.WordDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * 只读统计出口，给伴生模块（桌面小组件）用。
 *
 * 为什么要有它：桌面小组件的代码在 :companion 模块里，它拿不到 :app 的 Repository；
 * 但小组件更新时又与占位方（Launcher）无关、跑在本应用进程里，所以让 :app 暴露一个只读
 * ContentProvider 是最小的数据接口——伴生模块只查得到「总词数 / 今日待复习」两个数字，
 * 看不到表结构，也改不了任何数据。
 *
 * 用法：content://com.xucong.wordmemory.stats/summary → 一行两列 total、due。
 */
class WordStatsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.xucong.wordmemory.stats"
        const val PATH_SUMMARY = "summary"
        const val COLUMN_TOTAL = "total"
        const val COLUMN_DUE = "due"

        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_SUMMARY")
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val appContext = requireNotNull(context) { "ContentProvider 尚未附着到 Context" }
        val database = WordDatabase.get(appContext)
        val now = System.currentTimeMillis()

        val (total, due) = runBlocking {
            withContext(Dispatchers.IO) {
                database.wordDao().countAll() to database.wordDao().countDue(now)
            }
        }

        return MatrixCursor(arrayOf(COLUMN_TOTAL, COLUMN_DUE)).apply {
            addRow(arrayOf(total, due))
        }
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.$AUTHORITY.$PATH_SUMMARY"

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("统计接口只读")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("统计接口只读")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("统计接口只读")
}
