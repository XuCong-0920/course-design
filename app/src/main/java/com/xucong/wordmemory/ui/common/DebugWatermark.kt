package com.xucong.wordmemory.ui.common

import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.xucong.wordmemory.BuildConfig
import kotlin.math.roundToInt

/**
 * 录屏/答辩用的学号姓名水印，**只在 debug 包出现**。
 *
 * 做法：挂到 `android.R.id.content` 这个 FrameLayout 上，而不是往页面布局里塞 View——
 * 这样不参与任何界面的 measure/布局，不会把按钮挤动，也不会被 Fragment 切换丢掉。
 * 正式包（`BuildConfig.DEBUG == false`）里这个方法直接 return，release 包没有任何残留。
 */
object DebugWatermark {

    private const val TAG = "debug-watermark"

    /** 与交付要求一致：学号 + 姓名。 */
    private const val TEXT = "202405100228 徐聪"

    fun attachIfDebug(activity: Activity, text: String = TEXT) {
        Log.d(TAG, "attachIfDebug debug=${BuildConfig.DEBUG}")
        if (!BuildConfig.DEBUG) return

        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (content.findViewWithTag<TextView>(TAG) != null) return

        val watermark = TextView(activity).apply {
            tag = TAG
            this.text = text
            textSize = 13f
            setTextColor(Color.argb(0x66, 0xFF, 0xFF, 0xFF))
            setShadowLayer(2f, 0f, 1f, Color.argb(0x66, 0x00, 0x00, 0x00))
            isClickable = false
            isFocusable = false
            isEnabled = false
        }

        val density = activity.resources.displayMetrics.density
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            // 抬高 88dp：避开底部导航栏（80dp）与复习页的三档按钮
            bottomMargin = (88 * density).roundToInt()
            marginEnd = (12 * density).roundToInt()
        }
        content.addView(watermark, params)
        Log.d(TAG, "attached to ${activity.javaClass.simpleName}")
    }
}
