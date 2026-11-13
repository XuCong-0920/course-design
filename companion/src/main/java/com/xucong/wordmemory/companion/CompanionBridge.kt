package com.xucong.wordmemory.companion

/**
 * 伴生模块（《移动互联网应用开发实战》1 学分）与主 App 之间的**只读数据契约**。
 *
 * 为什么要这层接口，而不是让伴生模块直接依赖 Room：
 * 1. 模块可以单独编译、单独测试（不依赖主 App 的 Entity / DAO）；
 * 2. 依赖方向是单向的——主 App 依赖模块，模块只知道接口，不会反向依赖主 App；
 * 3. 将来把伴生模块拆成独立进程或单独 APK 时，这里换成 AIDL / ContentProvider 即可，
 *    上层调用方（`MineFragment`）不用改。
 */
interface DueDataProvider {

    /** 当前全部词库里的单词总数。 */
    suspend fun totalWordCount(): Int

    /** 指定时刻已经到期、需要复习的单词数。 */
    suspend fun dueCount(now: Long = System.currentTimeMillis()): Int
}

/**
 * 伴生模块对外的唯一入口。主 App 在 `Application.onCreate` 里 [bind] 一个实现，
 * 之后伴生模块内部（例如提醒 Worker 组装通知文案）就能读到主 App 的数据。
 */
object CompanionBridge {

    const val MODULE_NAME: String = "companion"

    /** 每日复习提醒使用的通知渠道 id。 */
    const val REMINDER_CHANNEL_ID: String = "word_review_reminder"

    /** 未绑定时的返回值：调用方可以据此显示「未连接」，而不是把 0 当成真实数据。 */
    const val NOT_BOUND: Int = -1

    @Volatile
    private var provider: DueDataProvider? = null

    /** 由主 App 在启动时注入；重复注入会被覆盖，便于测试时替换实现。 */
    fun bind(provider: DueDataProvider) {
        this.provider = provider
    }

    fun unbind() {
        provider = null
    }

    fun isReady(): Boolean = provider != null

    suspend fun totalWordCount(): Int = provider?.totalWordCount() ?: NOT_BOUND

    suspend fun dueCount(now: Long = System.currentTimeMillis()): Int =
        provider?.dueCount(now) ?: NOT_BOUND
}
