package com.example.class_project

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class IssueBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class BarEntry(val label: String, val count: Int, val color: Int)

    private var entries: List<BarEntry> = emptyList()
    private var animatedFraction = 1f

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6B7280.toInt()
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }
    private val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E3A5F.toInt()
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF0F0F0.toInt()
    }

    fun setEntries(entries: List<BarEntry>) {
        this.entries = entries
        animatedFraction = 0f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                this@IssueBarChartView.animatedFraction = animator.animatedFraction
                invalidate()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (180 * resources.displayMetrics.density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        if (entries.isEmpty()) return

        val maxCount = entries.maxOf { it.count }.coerceAtLeast(1)
        val barWidth = width / (entries.size * 2f)
        val topPad = 32f
        val bottomPad = 60f
        val maxBarHeight = height - topPad - bottomPad

        entries.forEachIndexed { index, entry ->
            val cx = width / entries.size * index + width / entries.size / 2f
            val barRight = cx + barWidth / 2
            val barLeft = cx - barWidth / 2
            val finalBarHeight = (entry.count.toFloat() / maxCount) * maxBarHeight
            val animBarHeight = finalBarHeight * animatedFraction
            val barTop = height - bottomPad - animBarHeight
            val finalBarTop = height - bottomPad - finalBarHeight
            val barBottom = height - bottomPad

            val bgRect = RectF(barLeft, topPad, barRight, barBottom)
            val radius = barWidth / 4
            canvas.drawRoundRect(bgRect, radius, radius, bgPaint)

            if (entry.count > 0) {
                barPaint.color = entry.color
                val rect = RectF(barLeft, barTop, barRight, barBottom)
                canvas.drawRoundRect(rect, radius, radius, barPaint)
            }

            countPaint.color = entry.color
            countPaint.alpha = (animatedFraction * 255).toInt()
            canvas.drawText(entry.count.toString(), cx, finalBarTop - 8f, countPaint)

            canvas.drawText(entry.label, cx, height - 12f, textPaint)
        }
    }
}
