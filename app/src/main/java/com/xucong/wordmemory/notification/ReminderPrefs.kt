package com.xucong.wordmemory.notification

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 提醒相关的小配置，用 SharedPreferences 存即可——只有「开关」「提醒时间」「今天别再提醒」
 * 三个短小的值，为此引入 DataStore 反而多一层依赖。
 */
object ReminderPrefs {

    /** 默认提醒时间：晚上 8 点（课程演示在晚上，容易当场触发）。 */
    const val DEFAULT_HOUR = 20

    private const val PREFS = "reminder_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour"
    private const val KEY_SKIP_DAY = "skip_day"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun hour(context: Context): Int = prefs(context).getInt(KEY_HOUR, DEFAULT_HOUR)

    fun setHour(context: Context, hour: Int) {
        prefs(context).edit().putInt(KEY_HOUR, hour.coerceIn(0, 23)).apply()
    }

    /** 通知上点「今天不提醒」后，当天剩余的触发都跳过（周期任务本身不动）。 */
    fun isSkippedToday(context: Context): Boolean =
        prefs(context).getString(KEY_SKIP_DAY, null) == today()

    fun skipToday(context: Context) {
        prefs(context).edit().putString(KEY_SKIP_DAY, today()).apply()
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
