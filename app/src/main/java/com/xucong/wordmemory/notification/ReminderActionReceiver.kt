package com.xucong.wordmemory.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * 通知上的两个「非打开 App」操作：
 *  - 10 分钟后：排一个一次性任务，到点重新检查并再提醒一次；
 *  - 今天不提醒：写今日跳过标记并撤掉当前通知，周期任务保留（明天照常）。
 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderNotifications.ACTION_SNOOZE -> {
                NotificationManagerCompat.from(context).cancel(ReminderNotifications.NOTIFICATION_ID)
                ReminderScheduler.snooze(context)
            }

            ReminderNotifications.ACTION_SKIP_TODAY -> {
                ReminderPrefs.skipToday(context)
                NotificationManagerCompat.from(context)
                    .cancel(ReminderNotifications.NOTIFICATION_ID)
            }
        }
    }
}
