package com.pocket4cut.frame

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Color as AColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

object CollageRenderer {

    private const val BRAND_TITLE = "Pocket 4Cut"
    private val brandTypeface: Typeface = Typeface.create("serif", Typeface.ITALIC)

    fun render(
        images: List<Bitmap>,
        frameStyle: FrameStyle,
        theme: FrameTheme,
        overrideBackground: Color?,
        filterId: FilterId,
        text: String?,
        dateString: String?,
        outputWidth: Int,
    ): Bitmap {
        val layout = collageLayoutForRender(frameStyle, theme, text, dateString, outputWidth)

        val bitmap = Bitmap.createBitmap(layout.canvasWidth.roundToInt(), layout.canvasHeight.roundToInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = overrideBackground ?: theme.background
        val cornerRadius = theme.cornerRadius * layout.scale
        val borderWidth = theme.borderWidth * layout.scale

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor.toArgb() }
        val canvasRect = RectF(0f, 0f, layout.canvasWidth, layout.canvasHeight)
        if (cornerRadius > 0f) {
            canvas.drawRoundRect(canvasRect, cornerRadius, cornerRadius, bgPaint)
        } else {
            canvas.drawRect(canvasRect, bgPaint)
        }

        if (borderWidth > 0f) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.border.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = borderWidth
            }
            val half = borderWidth / 2f
            val borderRect = RectF(half, half, layout.canvasWidth - half, layout.canvasHeight - half)
            if (cornerRadius > 0f) {
                val r = (cornerRadius - half).coerceAtLeast(0f)
                canvas.drawRoundRect(borderRect, r, r, borderPaint)
            } else {
                canvas.drawRect(borderRect, borderPaint)
            }
        }

        drawBrandTitle(canvas, layout.scale, layout.headerArea, bgColor)

        val colorFilter = FilterDefs.colorFilter(filterId)
        val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.colorFilter = colorFilter
        }
        val slotBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.argb(28, 0, 0, 0) }

        for ((i, cell) in layout.cells.withIndex()) {
            canvas.drawRect(cell, slotBg)
            images.getOrNull(i)?.let { drawAspectFill(canvas, it, cell, imgPaint) }
        }

        layout.textArea?.let { drawOverlayText(canvas, it, layout.scale, bgColor, text, dateString) }

        return bitmap
    }

    /* ── Drawing helpers ────────────────────────────────────────────── */

    private fun drawBrandTitle(canvas: Canvas, scale: Float, headerArea: RectF, bgColor: Color) {
        val textColor = if (isDark(bgColor)) AColor.WHITE else AColor.BLACK
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = BRAND_TITLE_TEXT_PT * scale
            typeface = brandTypeface
            textAlign = Paint.Align.CENTER
        }
        val area = headerArea
        val fm = paint.fontMetrics
        val y = area.top + (area.height() - fm.ascent - fm.descent) / 2f
        canvas.drawText(BRAND_TITLE, area.centerX(), y, paint)
    }

    private fun drawOverlayText(
        canvas: Canvas,
        area: RectF,
        scale: Float,
        bgColor: Color,
        text: String?,
        dateString: String?,
    ) {
        val parts = buildList {
            if (!text.isNullOrBlank()) add(text)
            if (!dateString.isNullOrBlank()) add(dateString)
        }
        if (parts.isEmpty()) return

        val joined = parts.joinToString("  \u00B7  ")
        val textColor = if (isDark(bgColor)) AColor.WHITE else AColor.BLACK
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 16f * scale
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val fm = paint.fontMetrics
        val y = area.top + (area.height() - fm.ascent - fm.descent) / 2f
        canvas.drawText(joined, area.centerX(), y, paint)
    }

    private fun drawAspectFill(canvas: Canvas, bitmap: Bitmap, dst: RectF, paint: Paint) {
        canvas.save()
        canvas.clipRect(dst)
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val s = maxOf(dst.width() / bw, dst.height() / bh)
        val sw = bw * s
        val sh = bh * s
        val l = dst.left + (dst.width() - sw) / 2f
        val t = dst.top + (dst.height() - sh) / 2f
        canvas.drawBitmap(bitmap, null, RectF(l, t, l + sw, t + sh), paint)
        canvas.restore()
    }

    private fun isDark(color: Color): Boolean {
        val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
        return luminance <= 0.5f
    }
}
