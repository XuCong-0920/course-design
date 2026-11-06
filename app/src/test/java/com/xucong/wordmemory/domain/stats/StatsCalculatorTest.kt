package com.xucong.wordmemory.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * StatsCalculator 的纯 JVM 单元测试：固定「今天 00:00」注入，
 * 因此不依赖运行时刻、时区或设备。
 */
class StatsCalculatorTest {

    /** 固定「今天」= 2023-11-14T00:00:00Z，方便用 day 的整数倍构造历史。 */
    private val DAY = StatsCalculator.DAY_MILLIS
    private val today = 1_700_000_000_000L - 1_700_000_000_000L % DAY

    private fun assertInt(expected: Int, actual: Int) {
        assertEquals(expected.toLong(), actual.toLong())
    }

    @Test
    fun `没有任何记录时 各项都是0 且 hasData 为假`() {
        val stats = StatsCalculator.build(emptyList(), today)

        assertInt(0, stats.todayTotal)
        assertInt(0, stats.streakDays)
        assertInt(0, stats.accuracyPercent)
        assertFalse(stats.hasData)
        assertInt(7, stats.days.size)
    }

    @Test
    fun `今日记录只落在最后一根柱子上`() {
        val events = listOf(
            ReviewEvent(today + 1_000, grade = 2),
            ReviewEvent(today + 2_000, grade = 1),
            ReviewEvent(today + 3_000, grade = 0),
        )

        val stats = StatsCalculator.build(events, today)

        assertInt(3, stats.todayTotal)
        assertInt(2, stats.todayCorrect) // grade 0 不算通过
        assertEquals(today, stats.days.last().dayStart)
        assertTrue(stats.days.take(6).all { it.total == 0 })
    }

    @Test
    fun `七天窗口按时间升序 最早的一天在最前面`() {
        val events = listOf(
            ReviewEvent(today - 6 * DAY + 5_000, grade = 2), // 六天前
            ReviewEvent(today - 1 * DAY + 5_000, grade = 2), // 昨天
            ReviewEvent(today + 5_000, grade = 2),           // 今天
        )

        val stats = StatsCalculator.build(events, today)

        assertInt(1, stats.days.first().total)
        assertEquals(today - 6 * DAY, stats.days.first().dayStart)
        assertInt(1, stats.days[5].total)
        assertInt(1, stats.days[6].total)
    }

    @Test
    fun `超出七天窗口的记录算进累计 但不占柱子`() {
        val old = ReviewEvent(today - 30 * DAY, grade = 2)

        val stats = StatsCalculator.build(listOf(old), today)

        assertInt(1, stats.totalReviewed)
        assertInt(0, stats.days.sumOf { it.total })
    }

    @Test
    fun `连续三天都有记录 打卡为3天`() {
        val events = listOf(
            ReviewEvent(today, grade = 2),
            ReviewEvent(today - DAY, grade = 2),
            ReviewEvent(today - 2 * DAY, grade = 2),
        )

        assertInt(3, StatsCalculator.build(events, today).streakDays)
    }

    @Test
    fun `今天还没复习但昨天复习过 连续打卡不断`() {
        val events = listOf(
            ReviewEvent(today - DAY, grade = 2),
            ReviewEvent(today - 2 * DAY, grade = 2),
        )

        assertInt(2, StatsCalculator.build(events, today).streakDays)
    }

    @Test
    fun `中间断了一天 连续打卡只算最近的一段`() {
        val events = listOf(
            ReviewEvent(today, grade = 2),
            ReviewEvent(today - DAY, grade = 2),
            ReviewEvent(today - 3 * DAY, grade = 2), // 少了前天，断开
            ReviewEvent(today - 4 * DAY, grade = 2),
        )

        assertInt(2, StatsCalculator.build(events, today).streakDays)
    }

    @Test
    fun `昨天和今天都没记录 连续打卡归零`() {
        val events = listOf(ReviewEvent(today - 2 * DAY, grade = 2))

        assertInt(0, StatsCalculator.build(events, today).streakDays)
    }

    @Test
    fun `正确率按 grade 大于等于1 统计`() {
        val events = listOf(
            ReviewEvent(today, grade = 0), // 不认识
            ReviewEvent(today, grade = 1), // 模糊
            ReviewEvent(today, grade = 2), // 认识
            ReviewEvent(today, grade = 2),
        )

        val stats = StatsCalculator.build(events, today)

        assertInt(3, stats.totalCorrect)
        assertInt(75, stats.accuracyPercent)
        assertInt(75, stats.days.last().accuracyPercent) // 当日 3/4
    }

    @Test
    fun `单日正确率在无记录时为0 不抛除零异常`() {
        val stats = StatsCalculator.build(emptyList(), today)

        assertInt(0, stats.days.first().accuracyPercent)
    }

    @Test
    fun `窗口天数可配置 用于将来做月视图`() {
        val stats = StatsCalculator.build(emptyList(), today, windowDays = 30)

        assertInt(30, stats.days.size)
        assertEquals(today - 29 * DAY, stats.days.first().dayStart)
    }
}
