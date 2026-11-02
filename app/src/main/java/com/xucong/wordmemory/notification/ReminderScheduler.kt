package com.xucong.wordmemory.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 提醒任务的排程入口。
 *
 * 用「周期任务 + 首次延迟到今天的 HH:00」而不是 AlarmManager：
 * 周期任务在重启、杀进程后由 WorkManager 自己恢复，不需要处理 BOOT_COMPLETED 和精确闹钟权限；
 * 代价是触发时间可能被系统推迟几分钟——对「每天提醒背单词」这个场景完全够用。
 */
object ReminderScheduler {

    private const val WORK_DAILY = "daily_review_reminder"
    private const val WORK_SNOOZE = "snooze_review_reminder"

    /** 幂等：重复调用只会更新已有的周期任务（UPDATE），不会叠加出多条提醒。 */
    fun scheduleDaily(context: Context, hour: Int = ReminderPrefs.hour(context)) {
        val request = PeriodicWorkRequestBuilder<ReviewReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNext(hour), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .addTag(WORK_DAILY)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_DAILY, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** 通知里点「10 分钟后」：只加一个一次性任务，周期任务不受影响。 */
    fun snooze(context: Context, minutes: Long = 10L) {
        val request = OneTimeWorkRequestBuilder<ReviewReminderWorker>()
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .addTag(WORK_SNOOZE)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_SNOOZE, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(WORK_DAILY)
            cancelUniqueWork(WORK_SNOOZE)
        }
    }

    /** 距离下一个 hour:00 还有多少毫秒（今天已过就顺延到明天）。 */
    private fun millisUntilNext(hour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}
