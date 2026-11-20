package com.xucong.wordmemory

import android.content.ContentResolver
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.xucong.wordmemory.companion.widget.ReviewWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 伴生模块（桌面小组件）读主 App 数据这条链路的仪器化验证。
 *
 * 小组件在真机上要长按桌面手动添加，不好自动化；这里直接验证它依赖的只读出口：
 * 1. ContentProvider 能返回 total / due 两列（这就是小组件上显示的两个数字）；
 * 2. 触发一次小组件刷新不会崩（即使当前桌面还没放小组件，refresh 也只是空跑）。
 */
@RunWith(AndroidJUnit4::class)
class WidgetDataTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver: ContentResolver = context.contentResolver
    private val uri = Uri.parse("content://com.xucong.wordmemory.stats/summary")

    @Test
    fun `统计出口返回总词数与今日待复习`() {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            assertTrue("查询结果应有至少一行", cursor.moveToFirst())

            val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
            val due = cursor.getInt(cursor.getColumnIndexOrThrow("due"))

            // 首次启动会播种两套示例词库（12 + 10），所以总数至少 22
            assertTrue("总词数应 >= 22，实际 $total", total >= 22)
            assertTrue("待复习数应落在 0..total，实际 $due / $total", due in 0..total)
        } ?: throw AssertionError("ContentProvider 返回了 null cursor")
    }

    @Test
    fun `统计出口是只读的`() {
        val inserted = runCatching {
            resolver.insert(uri, android.content.ContentValues().apply { put("total", 1) })
        }
        assertTrue("写入应被拒绝，实际返回 $inserted", inserted.isFailure)
    }

    @Test
    fun `刷新小组件不会崩溃`() {
        // 桌面没放小组件时 refresh 会直接返回；放了就会走一遍 RemoteViews 构建
        ReviewWidgetProvider.refresh(context)
        ReviewWidgetProvider.refresh(context)
    }
}
