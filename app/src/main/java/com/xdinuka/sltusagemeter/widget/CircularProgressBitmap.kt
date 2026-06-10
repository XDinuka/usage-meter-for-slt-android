package com.xdinuka.sltusagemeter.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Draws a circular progress arc into a [Bitmap].
 * Used by [SltMetricWidget] since Glance has no CircularProgressIndicator.
 */
object CircularProgressBitmap {
    fun create(
        sizePx: Int,
        progress: Float,        // 0.0 … 1.0
        progressColor: Int,     // android.graphics.Color ARGB int
        trackColor: Int,
        bgColor: Int,
        isDark: Boolean,
        labelText: String,
        valueText: String
    ): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val strokeW = sizePx / 9f
        val radius = cx - strokeW / 2f - 4f

        // Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, cx - 1f, bgPaint)

        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        // Track arc (270° sweep starting at 135°)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = trackColor
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(oval, 135f, 270f, false, trackPaint)

        // Progress arc
        val clampedProgress = progress.coerceIn(0f, 1f)
        if (clampedProgress > 0f) {
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = progressColor
                style = Paint.Style.STROKE
                strokeWidth = strokeW
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(oval, 135f, clampedProgress * 270f, false, progressPaint)
        }

        val textColor = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        val mutedColor = if (isDark) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY

        // Primary value text (e.g. "72%")
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            textSize = sizePx / 4.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        // Centre vertically between top of arc and bottom
        val valueY = cy + valuePaint.textSize * 0.35f
        canvas.drawText(valueText, cx, valueY, valuePaint)

        // Label text below value (e.g. "Peak")
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textAlign = Paint.Align.CENTER
            textSize = sizePx / 9f
        }
        val truncated = if (labelText.length > 10) "${labelText.take(9)}…" else labelText
        canvas.drawText(truncated, cx, valueY + labelPaint.textSize + 2f, labelPaint)

        return bmp
    }
}
