package com.pocket4cut.frame

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.pocket4cut.core.util.BitmapAdjustments

/**
 * Applies the non-destructive photo edits in the one order shared by preview and export.
 *
 * Ownership of [source] transfers to this function. Every replaced intermediate is recycled;
 * the returned bitmap belongs to the caller. Crop stays as renderer data and is not baked here.
 */
object PhotoEditPipeline {
    fun applyOwned(
        source: Bitmap,
        quarterTurnsClockwise: Int,
        flipHorizontal: Boolean,
        filterId: FilterId,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        checkCancelled: () -> Unit = {},
    ): Bitmap {
        var bitmap = source
        try {
            checkCancelled()
            repeat(((quarterTurnsClockwise % 4) + 4) % 4) {
                val rotated = BitmapAdjustments.rotate90(bitmap)
                if (rotated !== bitmap) bitmap.recycle()
                bitmap = rotated
                checkCancelled()
            }
            if (flipHorizontal) {
                val flipped = BitmapAdjustments.flipHorizontal(bitmap)
                if (flipped !== bitmap) bitmap.recycle()
                bitmap = flipped
                checkCancelled()
            }
            FilterDefs.colorFilter(filterId)?.let { colorFilter ->
                val filtered = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                try {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        this.colorFilter = colorFilter
                    }
                    Canvas(filtered).drawBitmap(bitmap, 0f, 0f, paint)
                } catch (cause: Throwable) {
                    filtered.recycle()
                    throw cause
                }
                bitmap.recycle()
                bitmap = filtered
                checkCancelled()
            }
            if (brightness != 0f || contrast != 1f || saturation != 1f) {
                val adjusted = BitmapAdjustments.applyColorAdjustments(
                    bitmap,
                    brightness,
                    contrast,
                    saturation,
                )
                if (adjusted !== bitmap) bitmap.recycle()
                bitmap = adjusted
                checkCancelled()
            }
            return bitmap
        } catch (cause: Throwable) {
            if (!bitmap.isRecycled) bitmap.recycle()
            throw cause
        }
    }
}
