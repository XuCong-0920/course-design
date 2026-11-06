package com.xucong.wordmemory.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors
import com.xucong.wordmemory.R
import com.xucong.wordmemory.domain.stats.DayStat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 近 7 天复习量柱状图。
 *
 * 没有引入 MPAndroidChart 之类的图表库：一张 7 根柱子的图用 Canvas 画不到 100 行，
 * 既省掉一个 300KB 的依赖，也不会因为库的 minSdk 或主题冲突在验收时翻车。
 */
class WeekChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(
            this@WeekChartView,
            androidx.appcompat.R.attr.colorPrimary,
        )
    }
    private val emptyBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(
            this@WeekChartView,
            com.google.android.material.R.attr.colorOutline,
        )
        alpha = BAR_ALPHA_EMPTY
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(
            this@WeekChartView,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
        )
        textAlign = Paint.Align.CENTER
        textSize = sp(11f)
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(
            this@WeekChartView,
            com.google.android.material.R.attr.colorOnSurface,
        )
        textAlign = Paint.Align.CENTER
        textSize = sp(12f)
    }
    private val barRect = RectF()
    private val dayFormat = SimpleDateFormat("M/d", Locale.CHINA)

    private var days: List<DayStat> = emptyList()

    fun setData(value: List<DayStat>) {
        days = value
        // 图表本身没有文字节点，给读屏/自动化测试一个可读的等价描述
        contentDescription = if (value.isEmpty()) {
            null
        } else {
            value.joinToString(prefix = "近 7 天复习量：", separator = "，") { day ->
                "${dayFormat.format(Date(day.dayStart))} ${day.total} 个"
            }
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val desiredHeight = dp(CHART_HEIGHT_DP).toInt() + (labelPaint.textSize * 2.6f).toInt()
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (days.isEmpty()) return

        val labelHeight = labelPaint.textSize + dp(6f)
        val valueHeight = valuePaint.textSize + dp(4f)
        val chartTop = paddingTop + valueHeight
        val chartBottom = height - paddingBottom - labelHeight
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)

        val slotWidth = (width - paddingLeft - paddingRight).toFloat() / days.size
        val barWidth = slotWidth * BAR_WIDTH_RATIO
        val maxTotal = days.maxOf { it.total }.coerceAtLeast(1)

        days.forEachIndexed { index, day ->
            val centerX = paddingLeft + slotWidth * (index + 0.5f)
            val ratio = day.total.toFloat() / maxTotal
            val barHeight = chartHeight * ratio
            val left = centerX - barWidth / 2f
            val right = centerX + barWidth / 2f
            val top = chartBottom - barHeight

            barRect.set(left, top, right, chartBottom)
            canvas.drawRoundRect(
                barRect,
                dp(BAR_CORNER_DP),
                dp(BAR_CORNER_DP),
                if (day.total == 0) emptyBarPaint else barPaint,
            )

            if (day.total > 0) {
                canvas.drawText(day.total.toString(), centerX, top - dp(4f), valuePaint)
            }
            canvas.drawText(
                dayFormat.format(Date(day.dayStart)),
                centerX,
                (height - paddingBottom).toFloat(),
                labelPaint,
            )
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_SP,
        value,
        resources.displayMetrics,
    )

    private companion object {
        const val CHART_HEIGHT_DP = 150f
        const val BAR_WIDTH_RATIO = 0.45f
        const val BAR_CORNER_DP = 4f
        const val BAR_ALPHA_EMPTY = 0x3D
    }
}
