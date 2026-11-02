package com.xucong.wordmemory.domain.srs

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SrsScheduler 的单元测试：纯 JVM，无 Android 依赖，`./gradlew :app:testDebugUnitTest` 直接跑。
 *
 * 时间全部用固定常量注入，"明天 / 后天"这类断言因此是确定性的，不受运行时刻影响。
 */
class SrsSchedulerTest {

    /** 固定注入时刻：2023-11-14T22:13:20Z。 */
    private val now = 1_700_000_000_000L
    private val day = SrsScheduler.DAY_MILLIS

    /** Int 断言统一走 long 重载，避免 JUnit 重载解析歧义。 */
    private fun assertInt(expected: Int, actual: Int) {
        assertEquals(expected.toLong(), actual.toLong())
    }

    @Test
    fun `新词第一次答对 间隔为1天 连续次数为1`() {
        val result = SrsScheduler.schedule(SrsState(dueAt = now), ReviewGrade.KNOWN, now)

        assertInt(1, result.repetition)
        assertInt(1, result.intervalDays)
        assertEquals(now + 1 * day, result.dueAt)
        assertEquals(2.6, result.easeFactor, 0.0001) // 2.5 + 0.1
    }

    @Test
    fun `连续答对第二次 间隔固定为3天`() {
        val first = SrsScheduler.schedule(SrsState(dueAt = now), ReviewGrade.KNOWN, now)
        val second = SrsScheduler.schedule(first, ReviewGrade.KNOWN, first.dueAt)

        assertInt(2, second.repetition)
        assertInt(3, second.intervalDays)
        assertEquals(first.dueAt + 3 * day, second.dueAt)
        assertEquals(2.7, second.easeFactor, 0.0001)
    }

    @Test
    fun `第三次答对起 间隔按旧间隔乘旧难度因子增长`() {
        var state = SrsState(dueAt = now)
        repeat(2) { state = SrsScheduler.schedule(state, ReviewGrade.KNOWN, now) }
        // 此时 repetition = 2，intervalDays = 3，easeFactor = 2.7

        val third = SrsScheduler.schedule(state, ReviewGrade.KNOWN, now)

        assertInt(3, third.repetition)
        assertInt(8, third.intervalDays) // round(3 × 2.7) = 8
        assertEquals(now + 8 * day, third.dueAt)
        assertEquals(2.8, third.easeFactor, 0.0001) // 2.7 + 0.1 夹到上限
    }

    @Test
    fun `答错时重置进度 10分钟后重来 难度因子下降`() {
        val learned = SrsScheduler.schedule(SrsState(dueAt = now), ReviewGrade.KNOWN, now)
        val forgot = SrsScheduler.schedule(learned, ReviewGrade.FORGOT, now + 1 * day)

        assertInt(0, forgot.repetition)
        assertInt(0, forgot.intervalDays)
        assertEquals(now + 1 * day + SrsScheduler.FORGOT_RETRY_MILLIS, forgot.dueAt)
        assertEquals(2.4, forgot.easeFactor, 0.0001) // 2.6 - 0.2
    }

    @Test
    fun `模糊算通过 但难度因子下降`() {
        val result = SrsScheduler.schedule(SrsState(dueAt = now), ReviewGrade.VAGUE, now)

        assertInt(1, result.repetition)
        assertInt(1, result.intervalDays)
        assertEquals(2.36, result.easeFactor, 0.0001) // 2.5 - 0.14
    }

    @Test
    fun `难度因子不会低于下限1_3`() {
        var state = SrsState(dueAt = now, easeFactor = 1.3)
        repeat(5) { state = SrsScheduler.schedule(state, ReviewGrade.FORGOT, now) }

        assertEquals(1.3, state.easeFactor, 0.0001)
    }

    @Test
    fun `难度因子不会高于上限2_8`() {
        var state = SrsState(dueAt = now, easeFactor = 2.8)
        repeat(5) { state = SrsScheduler.schedule(state, ReviewGrade.KNOWN, now) }

        assertEquals(2.8, state.easeFactor, 0.0001)
    }

    @Test
    fun `间隔不会超过180天上限`() {
        // 造一个间隔已经很长的词，连按公式增长会超出上限
        var state = SrsState(repetition = 10, intervalDays = 170, easeFactor = 2.8, dueAt = now)
        repeat(5) { state = SrsScheduler.schedule(state, ReviewGrade.KNOWN, now) }

        assertInt(SrsScheduler.MAX_INTERVAL_DAYS, state.intervalDays)
        assertEquals(now + 180 * day, state.dueAt)
    }

    @Test
    fun `跨闰日的到期时间按毫秒精确计算`() {
        // 2024-02-28T00:00:00Z，次日复习应落在 2024-02-29（闰日）
        val feb28 = 1_709_078_400_000L
        val result = SrsScheduler.schedule(SrsState(dueAt = feb28), ReviewGrade.KNOWN, feb28)

        assertEquals(feb28 + day, result.dueAt)
        assertEquals(1_709_164_800_000L, result.dueAt) // 2024-02-29T00:00:00Z
    }

    @Test
    fun `同一天内第二次答对 间隔仍是3天不会退化成0`() {
        val first = SrsScheduler.schedule(SrsState(dueAt = now), ReviewGrade.KNOWN, now)
        val again = SrsScheduler.schedule(first, ReviewGrade.KNOWN, now + 60_000L)

        assertInt(3, again.intervalDays)
    }

    @Test
    fun `三档反馈与数据库整数值一一对应`() {
        assertInt(0, ReviewGrade.FORGOT.toDbValue())
        assertInt(1, ReviewGrade.VAGUE.toDbValue())
        assertInt(2, ReviewGrade.KNOWN.toDbValue())

        assertEquals(ReviewGrade.FORGOT, ReviewGrade.fromDbValue(0))
        assertEquals(ReviewGrade.VAGUE, ReviewGrade.fromDbValue(1))
        assertEquals(ReviewGrade.KNOWN, ReviewGrade.fromDbValue(2))
    }
}
