package com.pocket4cut.frame

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.pocket4cut.presentation.navigation.FrameType
import kotlin.math.roundToInt

enum class RenderFilter {
    SOFT,
    FILM,
    BW,
}

object CollageRenderer {
    /**
     * targetWidth 기준으로 세로는 프레임 비율(9:16)로 고정한다.
     * 비트맵 개수가 부족하면 빈 슬롯은 그대로 둔다.
     */
    fun render(
        frameType: FrameType,
        theme: FrameTheme,
        bitmaps: List<Bitmap>,
        filter: RenderFilter,
        text: String?,
        dateText: String?,
        targetWidth: Int,
    ): Bitmap {
        val targetHeight = (targetWidth * 16f / 9f).roundToInt()
        val out = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // background
        canvas.drawColor(theme.background.toArgb())

        val padding = (targetWidth * 0.05f).roundToInt().toFloat()
        val gap = when (frameType) {
            FrameType.TWO_CUT -> (targetWidth * 0.03f).roundToInt().toFloat()
            FrameType.FOUR_CUT -> (targetWidth * 0.06f).roundToInt().toFloat()
            FrameType.SIX_CUT -> (targetWidth * 0.06f).roundToInt().toFloat()
        }
        val header = (targetHeight * 0.02f).roundToInt().toFloat()
        val footer = (targetHeight * 0.07f).roundToInt().toFloat()

        val contentLeft = padding
        val contentTop = padding + header
        val contentRight = targetWidth - padding
        val contentBottom = targetHeight - padding - footer

        val (cols, rows) = when (frameType) {
            FrameType.TWO_CUT -> 1 to 2
            FrameType.FOUR_CUT -> 2 to 2
            FrameType.SIX_CUT -> 2 to 3
        }

        val cellW = ((contentRight - contentLeft) - gap * (cols - 1)) / cols
        val cellH = ((contentBottom - contentTop) - gap * (rows - 1)) / rows

        val slotBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.argb(28, 0, 0, 0)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (targetWidth * 0.005f).coerceAtLeast(2f)
            color = theme.border.toArgb()
        }
        val slotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (targetWidth * 0.006f).coerceAtLeast(2f)
            color = theme.border.toArgb()
        }
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = filterToColorFilter(filter)
        }

        val radius = (targetWidth * 0.02f).coerceIn(12f, 28f)

        // slots
        var idx = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val left = contentLeft + c * (cellW + gap)
                val top = contentTop + r * (cellH + gap)
                val rect = RectF(left, top, left + cellW, top + cellH)
                canvas.drawRoundRect(rect, radius, radius, slotBgPaint)

                val bmp = bitmaps.getOrNull(idx)
                if (bmp != null) {
                    drawCenterCropClipped(canvas, bmp, rect, radius, imagePaint)
                }
                canvas.drawRoundRect(rect, radius, radius, slotBorderPaint)
                idx++
            }
        }

        // outer border
        val outerRadius = (targetWidth * 0.025f).coerceIn(14f, 34f)
        canvas.drawRoundRect(
            RectF(padding / 2f, padding / 2f, targetWidth - padding / 2f, targetHeight - padding / 2f),
            outerRadius,
            outerRadius,
            borderPaint,
        )

        // footer text/date
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.border.toArgb()
            textSize = (targetWidth * 0.05f).coerceAtLeast(16f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accent.toArgb()
            textSize = footerPaint.textSize
            typeface = footerPaint.typeface
        }

        val footerY = targetHeight - padding
        val leftX = padding
        val rightX = targetWidth - padding

        if (!text.isNullOrBlank()) {
            canvas.drawText(text, leftX, footerY, accentPaint)
        }

        if (!dateText.isNullOrBlank()) {
            val w = footerPaint.measureText(dateText)
            canvas.drawText(dateText, rightX - w, footerY, footerPaint)
        }

        return out
    }

    private fun filterToColorFilter(filter: RenderFilter): ColorMatrixColorFilter? {
        val matrix = when (filter) {
            RenderFilter.SOFT -> ColorMatrix().apply {
                // slight desaturation + brightness
                setSaturation(0.85f)
                postConcat(ColorMatrix(floatArrayOf(
                    1.05f, 0f, 0f, 0f, 8f,
                    0f, 1.05f, 0f, 0f, 8f,
                    0f, 0f, 1.05f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
            RenderFilter.FILM -> ColorMatrix().apply {
                setSaturation(0.9f)
                postConcat(ColorMatrix(floatArrayOf(
                    1.08f, 0.02f, 0.02f, 0f, 6f,
                    0.02f, 1.04f, 0.02f, 0f, 4f,
                    0.02f, 0.02f, 1.02f, 0f, 2f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
            RenderFilter.BW -> ColorMatrix().apply {
                setSaturation(0f)
                postConcat(ColorMatrix(floatArrayOf(
                    1.1f, 0f, 0f, 0f, 0f,
                    0f, 1.1f, 0f, 0f, 0f,
                    0f, 0f, 1.1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
        }
        return ColorMatrixColorFilter(matrix)
    }

    private fun drawCenterCropClipped(
        canvas: Canvas,
        bitmap: Bitmap,
        dst: RectF,
        cornerRadius: Float,
        paint: Paint,
    ) {
        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(dst, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val scale = maxOf(dst.width() / bw, dst.height() / bh)
        val sw = bw * scale
        val sh = bh * scale
        val left = dst.left + (dst.width() - sw) / 2f
        val top = dst.top + (dst.height() - sh) / 2f
        val rect = RectF(left, top, left + sw, top + sh)
        canvas.drawBitmap(bitmap, null, rect, paint)
        canvas.restore()
    }
}

