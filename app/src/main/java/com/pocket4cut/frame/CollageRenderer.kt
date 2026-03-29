package com.pocket4cut.frame

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

object CollageRenderer {
    private const val BRAND_NAME = "Pocket 4 Cut"

    fun render(
        frameStyle: FrameStyle,
        backgroundColor: androidx.compose.ui.graphics.Color,
        bitmaps: List<Bitmap>,
        filterId: FilterId,
        text: String?,
        dateText: String?,
        targetWidth: Int,
    ): Bitmap {
        val targetHeight = if (frameStyle.id == FrameLayoutId.FOUR_VERTICAL) {
            (targetWidth.toFloat() * 4920f / 1650f).roundToInt()
        } else {
            (targetWidth * 4f / 3f).roundToInt()
        }

        val out = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(backgroundColor.toArgb())

        val paddingFraction = frameStyle.padding / 400f
        val gapFraction = frameStyle.gap / 400f
        val padding = (targetWidth * paddingFraction).roundToInt().toFloat()
        val gap = (targetWidth * gapFraction).roundToInt().toFloat()

        val header = (targetHeight * 0.04f).roundToInt().toFloat()
        val footer = (targetHeight * 0.05f).roundToInt().toFloat()

        val contentLeft = padding
        val contentTop = padding + header
        val contentRight = targetWidth - padding
        val contentBottom = targetHeight - padding - footer

        val colorFilter = FilterDefs.colorFilter(filterId)
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.colorFilter = colorFilter
        }
        val slotBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.argb(28, 0, 0, 0)
        }

        val isLightBg = isLightColor(backgroundColor)
        val textColorInt = if (isLightBg) AColor.argb(200, 0, 0, 0) else AColor.argb(200, 255, 255, 255)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorInt
            textSize = (header * 0.55f).coerceIn(14f, 40f)
            typeface = Typeface.create("serif", Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(BRAND_NAME, targetWidth / 2f, padding + header * 0.72f, brandPaint)

        when (frameStyle.layout) {
            LayoutType.VERTICAL -> drawVerticalSlots(canvas, bitmaps, frameStyle, contentLeft, contentTop, contentRight, contentBottom, gap, slotBgPaint, imagePaint)
            LayoutType.HORIZONTAL -> drawHorizontalSlots(canvas, bitmaps, frameStyle, contentLeft, contentTop, contentRight, contentBottom, gap, slotBgPaint, imagePaint)
            LayoutType.GRID -> drawGridSlots(canvas, bitmaps, frameStyle, contentLeft, contentTop, contentRight, contentBottom, gap, slotBgPaint, imagePaint)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColorInt
            textSize = (targetWidth * 0.035f).coerceAtLeast(14f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val footerY = targetHeight - padding * 0.5f
        if (!text.isNullOrBlank()) {
            canvas.drawText(text, padding, footerY, footerPaint)
        }
        if (!dateText.isNullOrBlank()) {
            val w = footerPaint.measureText(dateText)
            canvas.drawText(dateText, targetWidth - padding - w, footerY, footerPaint)
        }

        return out
    }

    private fun drawVerticalSlots(
        canvas: Canvas, bitmaps: List<Bitmap>, style: FrameStyle,
        left: Float, top: Float, right: Float, bottom: Float,
        gap: Float, bgPaint: Paint, imgPaint: Paint,
    ) {
        val slots = style.slots
        val totalGap = gap * (slots - 1)
        val cellH = ((bottom - top) - totalGap) / slots
        val cellW = right - left
        for (i in 0 until slots) {
            val y = top + i * (cellH + gap)
            val rect = RectF(left, y, left + cellW, y + cellH)
            canvas.drawRect(rect, bgPaint)
            bitmaps.getOrNull(i)?.let { drawCenterCropClipped(canvas, it, rect, imgPaint) }
        }
    }

    private fun drawHorizontalSlots(
        canvas: Canvas, bitmaps: List<Bitmap>, style: FrameStyle,
        left: Float, top: Float, right: Float, bottom: Float,
        gap: Float, bgPaint: Paint, imgPaint: Paint,
    ) {
        val slots = style.slots
        val totalGap = gap * (slots - 1)
        val cellW = ((right - left) - totalGap) / slots
        val cellH = bottom - top
        for (i in 0 until slots) {
            val x = left + i * (cellW + gap)
            val rect = RectF(x, top, x + cellW, top + cellH)
            canvas.drawRect(rect, bgPaint)
            bitmaps.getOrNull(i)?.let { drawCenterCropClipped(canvas, it, rect, imgPaint) }
        }
    }

    private fun drawGridSlots(
        canvas: Canvas, bitmaps: List<Bitmap>, style: FrameStyle,
        left: Float, top: Float, right: Float, bottom: Float,
        gap: Float, bgPaint: Paint, imgPaint: Paint,
    ) {
        val cols = style.gridColumns
        val rows = style.gridRows
        val cellW = ((right - left) - gap * (cols - 1)) / cols
        val cellH = ((bottom - top) - gap * (rows - 1)) / rows
        var idx = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = left + c * (cellW + gap)
                val y = top + r * (cellH + gap)
                val rect = RectF(x, y, x + cellW, y + cellH)
                canvas.drawRect(rect, bgPaint)
                bitmaps.getOrNull(idx)?.let { drawCenterCropClipped(canvas, it, rect, imgPaint) }
                idx++
            }
        }
    }

    private fun isLightColor(color: androidx.compose.ui.graphics.Color): Boolean {
        val r = color.red
        val g = color.green
        val b = color.blue
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        return luminance > 0.5f
    }

    private fun drawCenterCropClipped(canvas: Canvas, bitmap: Bitmap, dst: RectF, paint: Paint) {
        canvas.save()
        canvas.clipRect(dst)
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val scale = maxOf(dst.width() / bw, dst.height() / bh)
        val sw = bw * scale
        val sh = bh * scale
        val l = dst.left + (dst.width() - sw) / 2f
        val t = dst.top + (dst.height() - sh) / 2f
        canvas.drawBitmap(bitmap, null, RectF(l, t, l + sw, t + sh), paint)
        canvas.restore()
    }
}
