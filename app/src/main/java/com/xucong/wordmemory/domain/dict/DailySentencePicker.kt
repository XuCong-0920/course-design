package com.xucong.wordmemory.domain.dict

/**
 * 按「日期 → 一条」的确定性选择：同一天打开多少次都是同一句，跨天才换。
 *
 * 为什么不用随机：随机会让「每日例句」在同一页面刷新时跳动，
 * 而且复习模块强调「今天该做什么是确定的」，这里保持一致的语义。
 */
object DailySentencePicker {

    /** 一天里挑中的条目：用日期做下标，负数也安全，空列表返回 null。 */
    fun <T> pick(candidates: List<T>, dayIndex: Long): T? {
        if (candidates.isEmpty()) return null
        val size = candidates.size
        val index = ((dayIndex % size) + size) % size
        return candidates[index.toInt()]
    }

    /** 由「今天的 0 点」换算出第几天，跨时区/夏令时的误差对「每天换一句」无影响。 */
    fun dayIndex(dayStartMillis: Long, dayMillis: Long = 86_400_000L): Long =
        Math.floorDiv(dayStartMillis, dayMillis)
}
