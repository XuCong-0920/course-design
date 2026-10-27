package com.xucong.wordmemory.companion

/**
 * 伴生模块（《移动互联网应用开发实战》1 学分）对主 App 暴露的统一入口。
 *
 * 骨架阶段只有常量与开关；第 7 次课起在此实现每日复习提醒的通知渠道、
 * WorkManager 调度与通知栏三 Action（记得 / 稍后 / 跳过）。
 */
object CompanionBridge {

    const val MODULE_NAME: String = "companion"

    /** 每日复习提醒使用的通知渠道 id。 */
    const val REMINDER_CHANNEL_ID: String = "word_review_reminder"

    /** 骨架阶段尚未接入任何能力。 */
    fun isReady(): Boolean = false
}
