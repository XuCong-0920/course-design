package com.xucong.wordmemory.domain.srs

/**
 * 复习反馈三档。与数据库 `review_records.grade` 的 0/1/2 一一对应，
 * 映射见 [toDbValue] / [fromDbValue]，避免 UI 层直接写裸数字。
 */
enum class ReviewGrade {
    /** 不认识：本次算失败，进度重置、10 分钟后重来。 */
    FORGOT,

    /** 模糊：算勉强通过，间隔增长但难度因子下降。 */
    VAGUE,

    /** 认识：算顺利通过，间隔增长且难度因子上升。 */
    KNOWN,
    ;

    /** SM-2 的"质量分" q：不认识 2、模糊 3、认识 5。 */
    fun quality(): Int = when (this) {
        FORGOT -> 2
        VAGUE -> 3
        KNOWN -> 5
    }

    fun toDbValue(): Int = when (this) {
        FORGOT -> 0
        VAGUE -> 1
        KNOWN -> 2
    }

    companion object {
        fun fromDbValue(value: Int): ReviewGrade = when (value) {
            0 -> FORGOT
            1 -> VAGUE
            else -> KNOWN
        }
    }
}

/**
 * 调度器的输入/输出状态，只包含与算法有关的四个字段。
 *
 * 刻意不直接用 `WordEntity`：算法层不依赖 Room（也不依赖 Android），
 * 这样 `SrsScheduler` 可以在纯 JVM 单元测试里跑，不需要 Robolectric 或设备。
 */
data class SrsState(
    /** 连续答对次数（SM-2 的 repetition）。 */
    val repetition: Int = 0,
    /** 当前间隔天数；0 表示新词、尚未安排。 */
    val intervalDays: Int = 0,
    /** 难度因子（SM-2 的 easiness factor）。 */
    val easeFactor: Double = SrsScheduler.DEFAULT_EASE_FACTOR,
    /** 下次到期时间（UTC 毫秒时间戳）；0 表示"立即可复习"。 */
    val dueAt: Long = 0L,
)

/**
 * 简化的 SM-2 调度算法（B4 的"自定义算法"技术点）。
 *
 * 与标准 SM-2 的差别，以及为什么这样改：
 * - 反馈只有三档（认识/模糊/不认识），而不是 SM-2 的 0–5 六档：按钮越少，复习时越快，
 *   手机端三档是主流做法；三档映射到 q = 5 / 3 / 2。
 * - 失败的「重来」间隔固定为 10 分钟，而不是 1 天：一节课内就能再见到这个错词，
 *   符合课堂演示场景（也是 Anki 的默认学习步长）。
 * - 间隔在 1 天 / 3 天两档之后才按 `间隔 × 难度因子` 增长，并夹在 1–180 天：
 *   上限 180 天避免间隔无限膨胀后"再也见不到这个词"。
 *
 * 全部是纯函数：无 IO、无 `System.currentTimeMillis()`，时间由调用方以 `now` 注入，
 * 因此可以精确断言"跨月/跨年"这类时间计算。
 */
object SrsScheduler {

    const val DEFAULT_EASE_FACTOR = 2.5
    const val MIN_EASE_FACTOR = 1.3
    const val MAX_EASE_FACTOR = 2.8
    const val MAX_INTERVAL_DAYS = 180

    /** 答错后多久重来：10 分钟。 */
    const val FORGOT_RETRY_MILLIS = 10 * 60 * 1000L

    /** 一天的毫秒数。算法统一按 24 小时记，展示时再本地化。 */
    const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    /** 答错时的难度因子惩罚。 */
    private const val FORGOT_EASE_PENALTY = 0.2

    /**
     * 计算下一次复习状态。
     *
     * @param state 当前状态（新词传 [SrsState] 默认值即可）
     * @param grade 本次反馈档位
     * @param now 本次复习时刻（毫秒时间戳），由调用方注入以便测试
     * @return 新的 [SrsState]；调用方据此更新 `WordEntity` 并写入 `review_records`
     */
    fun schedule(state: SrsState, grade: ReviewGrade, now: Long): SrsState {
        val quality = grade.quality()

        // 1) 失败分支：重置进度，10 分钟后重来，难度因子小幅下降。
        if (quality < 3) {
            return state.copy(
                repetition = 0,
                intervalDays = 0,
                easeFactor = clampEase(state.easeFactor - FORGOT_EASE_PENALTY),
                dueAt = now + FORGOT_RETRY_MILLIS,
            )
        }

        // 2) 通过分支：先按"旧间隔 × 旧难度因子"算新间隔，再更新难度因子。
        val repetition = state.repetition + 1
        val intervalDays = when (repetition) {
            1 -> 1
            2 -> 3
            else -> (state.intervalDays * state.easeFactor)
                .toLong()
                .coerceIn(1L, MAX_INTERVAL_DAYS.toLong())
                .toInt()
        }

        return SrsState(
            repetition = repetition,
            intervalDays = intervalDays,
            easeFactor = clampEase(state.easeFactor + easeDelta(quality)),
            dueAt = now + intervalDays * DAY_MILLIS,
        )
    }

    /** SM-2 的难度因子增量：ΔEF = 0.1 - (5-q) × (0.08 + (5-q) × 0.02)。 */
    private fun easeDelta(quality: Int): Double {
        val diff = 5 - quality
        return 0.1 - diff * (0.08 + diff * 0.02)
    }

    /** 夹在 [MIN_EASE_FACTOR, MAX_EASE_FACTOR]，并保留两位小数（便于入库与展示）。 */
    private fun clampEase(value: Double): Double =
        (Math.round(value.coerceIn(MIN_EASE_FACTOR, MAX_EASE_FACTOR) * 100.0) / 100.0)
}
