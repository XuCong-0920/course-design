package com.xucong.wordmemory.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.xucong.wordmemory.MainActivity
import com.xucong.wordmemory.R

/**
 * 复习提醒通知的构建与下发。
 *
 * 三个 Action 的分工：
 *  - 去复习：直接打开 App 并落在复习页（不是首页，少一次点击）；
 *  - 10 分钟后：把同一条提醒延迟重发一次（OneTimeWorkRequest，不入库、不改周期任务）；
 *  - 今天不提醒：只写一个「今日跳过」标记，周期任务继续留着，明天照常提醒。
 */
object ReminderNotifications {

    const val CHANNEL_ID = "review_reminder"
    const val NOTIFICATION_ID = 1001

    const val ACTION_SNOOZE = "com.xucong.wordmemory.action.SNOOZE_REMINDER"
    const val ACTION_SKIP_TODAY = "com.xucong.wordmemory.action.SKIP_TODAY"

    /** MainActivity 收到这个 extra 就直接切到复习页。 */
    const val EXTRA_OPEN_REVIEW = "open_review"

    private const val PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    /** Android 13+ 需要 POST_NOTIFICATIONS 运行时授权；更早版本看系统总开关。 */
    fun canPost(context: Context): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showReminder(context: Context, dueCount: Int) {
        if (!canPost(context)) return
        ensureChannel(context)

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_REVIEW, true)
        }
        val openPi = PendingIntent.getActivity(context, 10, openApp, PI_FLAGS)

        val snoozePi = PendingIntent.getBroadcast(
            context,
            11,
            Intent(context, ReminderActionReceiver::class.java).setAction(ACTION_SNOOZE),
            PI_FLAGS,
        )
        val skipPi = PendingIntent.getBroadcast(
            context,
            12,
            Intent(context, ReminderActionReceiver::class.java).setAction(ACTION_SKIP_TODAY),
            PI_FLAGS,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title, dueCount))
            .setContentText(context.getString(R.string.reminder_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .addAction(0, context.getString(R.string.reminder_action_review), openPi)
            .addAction(0, context.getString(R.string.reminder_action_snooze), snoozePi)
            .addAction(0, context.getString(R.string.reminder_action_skip), skipPi)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
