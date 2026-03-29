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

    private data class Layout(
        val canvasWidth: Int,
        val canvasHeight: Int,
        val scale: Float,
        val cells: List<RectF>,
        val headerArea: RectF,
        val textArea: RectF?,
    )

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
        val layout = if (frameStyle.id == FrameLayoutId.FOUR_VERTICAL) {
            computeFourVerticalLayout(frameStyle, theme, text, dateString)
        } else {
            computeLayout(frameStyle, theme, text, dateString, outputWidth)
        }

        val bitmap = Bitmap.createBitmap(layout.canvasWidth, layout.canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = overrideBackground ?: theme.background
        val cornerRadius = theme.cornerRadius * layout.scale
        val borderWidth = theme.borderWidth * layout.scale

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor.toArgb() }
        val canvasRect = RectF(0f, 0f, layout.canvasWidth.toFloat(), layout.canvasHeight.toFloat())
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

        drawBrandTitle(canvas, layout, bgColor)

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

    /* ── Four-vertical fixed-size layout: 1650×4920 ─────────────────── */

    private fun computeFourVerticalLayout(
        style: FrameStyle,
        theme: FrameTheme,
        text: String?,
        dateString: String?,
    ): Layout {
        val w = 1650f
        val h = 4920f
        val scale = w / 390f
        val rows = style.rows

        val hasText = !text.isNullOrBlank() || !dateString.isNullOrBlank()
        val headerHeight = 90f * scale
        val textAreaHeight = if (hasText) 80f * scale else 0f
        val padding = theme.outerPadding * scale
        val spacing = theme.cellSpacing * scale

        val contentWidth = w - padding * 2f
        val rowGaps = spacing * (rows - 1)
        val chrome = headerHeight + padding * 2f + textAreaHeight + rowGaps
        val cellHeight = (h - chrome) / rows

        val cells = buildList {
            for (r in 0 until rows) {
                val x = padding
                val y = padding + headerHeight + r * (cellHeight + spacing)
                add(RectF(x, y, x + contentWidth, y + cellHeight))
            }
        }

        return Layout(
            canvasWidth = w.roundToInt(),
            canvasHeight = h.roundToInt(),
            scale = scale,
            cells = cells,
            headerArea = RectF(0f, 0f, w, padding + headerHeight),
            textArea = if (hasText) RectF(0f, h - textAreaHeight - padding, w, h) else null,
        )
    }

    /* ── Generic layout (all layouts except four-vertical) ──────────── */

    private fun computeLayout(
        style: FrameStyle,
        theme: FrameTheme,
        text: String?,
        dateString: String?,
        outputWidth: Int,
    ): Layout {
        val w = outputWidth.toFloat()
        val scale = w / 390f
        val rows = style.rows
        val cols = style.columns

        val hasText = !text.isNullOrBlank() || !dateString.isNullOrBlank()
        val headerHeight = 90f * scale
        val textAreaHeight = if (hasText) 80f * scale else 0f
        val padding = theme.outerPadding * scale
        val spacing = theme.cellSpacing * scale

        val contentWidth = w - padding * 2f
        val cellAspect = style.cellAspectWidthOverHeight
        val cellWidth = (contentWidth - spacing * (cols - 1)) / cols
        val cellHeight = cellWidth / cellAspect
        val contentHeight = cellHeight * rows + spacing * (rows - 1)
        val canvasHeight = headerHeight + contentHeight + padding * 2f + textAreaHeight

        val cells = buildList {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x = padding + c * (cellWidth + spacing)
                    val y = padding + headerHeight + r * (cellHeight + spacing)
                    add(RectF(x, y, x + cellWidth, y + cellHeight))
                }
            }
        }

        return Layout(
            canvasWidth = w.roundToInt(),
            canvasHeight = canvasHeight.roundToInt(),
            scale = scale,
            cells = cells,
            headerArea = RectF(0f, 0f, w, padding + headerHeight),
            textArea = if (hasText) RectF(0f, canvasHeight - textAreaHeight - padding, w, canvasHeight) else null,
        )
    }

    /* ── Drawing helpers ────────────────────────────────────────────── */

    private fun drawBrandTitle(canvas: Canvas, layout: Layout, bgColor: Color) {
        val textColor = if (isDark(bgColor)) AColor.WHITE else AColor.BLACK
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 44f * layout.scale
            typeface = brandTypeface
            textAlign = Paint.Align.CENTER
        }
        val area = layout.headerArea
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
