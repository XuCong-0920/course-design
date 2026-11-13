package com.xucong.wordmemory.companion

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CompanionBridge 的契约测试：未绑定时必须返回 [CompanionBridge.NOT_BOUND]，
 * 而不是把 0 当成真实数据——「0 个待复习」和「没接上」是两种完全不同的状态，
 * 界面上必须能区分（否则伴生模块没接好也看不出来）。
 */
class CompanionBridgeTest {

    private class FakeProvider(
        private val total: Int,
        private val due: Int,
    ) : DueDataProvider {
        override suspend fun totalWordCount(): Int = total
        override suspend fun dueCount(now: Long): Int = due
    }

    @After
    fun tearDown() {
        CompanionBridge.unbind()
    }

    @Test
    fun `未绑定时 isReady 为假 且数值是 NOT_BOUND`() = runBlocking {
        CompanionBridge.unbind()

        assertFalse(CompanionBridge.isReady())
        assertEquals(CompanionBridge.NOT_BOUND, CompanionBridge.totalWordCount())
        assertEquals(CompanionBridge.NOT_BOUND, CompanionBridge.dueCount(0L))
    }

    @Test
    fun `绑定后读到主 App 注入的数据`() = runBlocking {
        CompanionBridge.bind(FakeProvider(total = 22, due = 20))

        assertTrue(CompanionBridge.isReady())
        assertEquals(22, CompanionBridge.totalWordCount())
        assertEquals(20, CompanionBridge.dueCount(1_700_000_000_000L))
    }

    @Test
    fun `重复绑定以最后一次为准`() = runBlocking {
        CompanionBridge.bind(FakeProvider(total = 22, due = 20))
        CompanionBridge.bind(FakeProvider(total = 30, due = 0))

        assertEquals(30, CompanionBridge.totalWordCount())
        assertEquals(0, CompanionBridge.dueCount(0L))
    }

    @Test
    fun `解绑后回到未连接状态`() = runBlocking {
        CompanionBridge.bind(FakeProvider(total = 1, due = 1))
        CompanionBridge.unbind()

        assertFalse(CompanionBridge.isReady())
        assertEquals(CompanionBridge.NOT_BOUND, CompanionBridge.dueCount(0L))
    }

    @Test
    fun `模块名与通知渠道 id 保持稳定`() {
        // 通知渠道一旦发布就不能改 id：改了等于给老用户建新渠道，旧设置全部失效
        assertEquals("companion", CompanionBridge.MODULE_NAME)
        assertEquals("word_review_reminder", CompanionBridge.REMINDER_CHANNEL_ID)
    }
}
