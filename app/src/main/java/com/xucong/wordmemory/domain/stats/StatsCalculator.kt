package com.xucong.wordmemory.domain.stats

/**
 * 一条复习事件的最小投影：只保留算统计需要的两个字段，
 * 让统计逻辑不依赖 Room 实体，可以在纯 JVM 单元测试里直接跑。
 */
data class ReviewEvent(val reviewedAt: Long, val grade: Int)

/** 单日统计。[dayStart] 是该日 00:00 的时间戳。 */
data class DayStat(val dayStart: Long, val total: Int, val correct: Int) {
    /** 当日正确率，0–100；当日没复习时定义为 0（由调用方决定要不要显示）。 */
    val accuracyPercent: Int get() = if (total == 0) 0 else correct * 100 / total
}

data class StudyStats(
    val todayTotal: Int = 0,
    val todayCorrect: Int = 0,
    val totalReviewed: Int = 0,
    val totalCorrect: Int = 0,
    val streakDays: Int = 0,
    val days: List<DayStat> = emptyList(),
) {
    val accuracyPercent: Int get() = if (totalReviewed == 0) 0 else totalCorrect * 100 / totalReviewed
    val hasData: Boolean get() = totalReviewed > 0
}

/**
 * 学习统计的纯计算部分：把复习记录切成「按天」的桶，再算连续打卡与正确率。
 *
 * 不直接读数据库，也不依赖时区 API —— 调用方传入今天 00:00 的时间戳与一天的长度，
 * 这样跨时区、跨夏令时的边界由调用方掌握，这里只做确定性的算术（也方便写 11 个单测）。
 */
object StatsCalculator {

    const val DAY_MILLIS = 86_400_000L
    const val WINDOW_DAYS = 7

    /** grade >= 1（模糊 / 认识）算通过，与 ReviewGrade.toDbValue() 的约定一致。 */
    private const val PASS_GRADE = 1

    fun build(
        events: List<ReviewEvent>,
        todayStart: Long,
        dayMillis: Long = DAY_MILLIS,
        windowDays: Int = WINDOW_DAYS,
    ): StudyStats {
        require(windowDays > 0) { "windowDays must be positive" }

        val totals = HashMap<Long, Int>()
        val corrects = HashMap<Long, Int>()
        events.forEach { event ->
            val day = dayStartOf(event.reviewedAt, todayStart, dayMillis)
            totals[day] = (totals[day] ?: 0) + 1
            if (event.grade >= PASS_GRADE) {
                corrects[day] = (corrects[day] ?: 0) + 1
            }
        }

        // 按时间升序，最后一个是今天 —— 画柱状图时直接按顺序摆即可
        val days = (windowDays - 1 downTo 0).map { offset ->
            val start = todayStart - offset * dayMillis
            DayStat(dayStart = start, total = totals[start] ?: 0, correct = corrects[start] ?: 0)
        }

        return StudyStats(
            todayTotal = days.last().total,
            todayCorrect = days.last().correct,
            totalReviewed = events.size,
            totalCorrect = events.count { it.grade >= PASS_GRADE },
            streakDays = streakDays(totals, todayStart, dayMillis),
            days = days,
        )
    }

    /** 把任意时间戳落到它所属那天的 00:00（向下取整，负数时间也正确）。 */
    private fun dayStartOf(timestamp: Long, todayStart: Long, dayMillis: Long): Long {
        val diff = timestamp - todayStart
        val index = if (diff >= 0) diff / dayMillis else -((-diff + dayMillis - 1) / dayMillis)
        return todayStart + index * dayMillis
    }

    /**
     * 连续打卡天数。今天已经复习就从今天往回数；今天还没复习但昨天复习过，
     * 从昨天往回数（当天没结束不算断签）。
     */
    private fun streakDays(totals: Map<Long, Int>, todayStart: Long, dayMillis: Long): Int {
        var cursor = if ((totals[todayStart] ?: 0) > 0) todayStart else todayStart - dayMillis
        var streak = 0
        while ((totals[cursor] ?: 0) > 0) {
            streak++
            cursor -= dayMillis
        }
        return streak
    }
}
