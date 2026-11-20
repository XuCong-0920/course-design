package com.xucong.wordmemory.companion.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.xucong.wordmemory.companion.R

/**
 * 伴生模块的独立入口之一：主屏小组件。
 *
 * 它自己就能显示「今日待复习 / 总词数」，点「去复习」直接打开主 App 的复习页，
 * 点「刷新」重新读一次主 App 的数据——不需要主 App 界面在前台。
 *
 * 数据来自主 App 暴露的只读 ContentProvider：
 * content://com.xucong.wordmemory.stats/summary（total、due 两列）。
 */
class ReviewWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refresh(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.xucong.wordmemory.companion.action.REFRESH_WIDGET"

        private const val EXTRA_OPEN_REVIEW = "open_review"
        private const val MAIN_ACTIVITY = "com.xucong.wordmemory.MainActivity"
        private const val STATS_URI = "content://com.xucong.wordmemory.stats/summary"
        private const val UNKNOWN = -1

        /** 主 App 回到前台时调用，让小组件数字立刻跟上。 */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, ReviewWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { id -> manager.updateAppWidget(id, buildViews(context)) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_review)
            val (total, due) = readCounts(context)

            views.setTextViewText(
                R.id.widgetDueText,
                if (due >= 0) context.getString(R.string.widget_due_value, due)
                else context.getString(R.string.widget_unavailable),
            )
            views.setTextViewText(
                R.id.widgetTotalText,
                if (total >= 0) context.getString(R.string.widget_total_value, total)
                else context.getString(R.string.widget_unavailable),
            )

            views.setOnClickPendingIntent(R.id.widgetOpenButton, openReviewIntent(context))
            views.setOnClickPendingIntent(R.id.widgetRefreshButton, refreshIntent(context))
            return views
        }

        /** 读主 App 的只读统计；读不到（例如主 App 被卸载）就返回 -1，界面显示“暂时读不到”。 */
        private fun readCounts(context: Context): Pair<Int, Int> = runCatching {
            context.contentResolver.query(Uri.parse(STATS_URI), null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    UNKNOWN to UNKNOWN
                } else {
                    val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
                    val due = cursor.getInt(cursor.getColumnIndexOrThrow("due"))
                    total to due
                }
            } ?: (UNKNOWN to UNKNOWN)
        }.getOrDefault(UNKNOWN to UNKNOWN)

        private fun openReviewIntent(context: Context): PendingIntent {
            val intent = Intent().apply {
                setClassName(context.packageName, MAIN_ACTIVITY)
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                putExtra(EXTRA_OPEN_REVIEW, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun refreshIntent(context: Context): PendingIntent {
            val intent = Intent(context, ReviewWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            return PendingIntent.getBroadcast(
                context,
                1002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
