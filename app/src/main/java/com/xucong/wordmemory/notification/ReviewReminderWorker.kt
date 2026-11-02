package com.xucong.wordmemory.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xucong.wordmemory.data.local.WordDatabase

/**
 * 到点检查「有没有该复习的词」，有才发通知——避免每天固定弹一条空提醒，
 * 用户对「点了没东西可看」的通知最反感，也最容易顺手关掉通知权限。
 */
class ReviewReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        if (!ReminderPrefs.isEnabled(context)) return Result.success()
        if (ReminderPrefs.isSkippedToday(context)) return Result.success()

        val dueCount = WordDatabase.get(context).wordDao().countDue(System.currentTimeMillis())
        if (dueCount <= 0) return Result.success()

        ReminderNotifications.showReminder(context, dueCount)
        return Result.success()
    }
}
