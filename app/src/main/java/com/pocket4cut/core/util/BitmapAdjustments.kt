package com.pocket4cut.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint

object BitmapAdjustments {

    private fun asSoftwareArgb(source: Bitmap): Bitmap {
        if (source.config != Bitmap.Config.HARDWARE) return source
        return source.copy(Bitmap.Config.ARGB_8888, false)
    }

    fun rotate90(source: Bitmap): Bitmap {
        val safe = asSoftwareArgb(source)
        val m = Matrix().apply { postRotate(90f) }
        val out = Bitmap.createBitmap(safe, 0, 0, safe.width, safe.height, m, true)
        if (safe !== source) safe.recycle()
        return out
    }

    /** brightness: roughly -0.35 .. 0.35, contrast: roughly 0.7 .. 1.5 (1 = unchanged) */
    fun applyBrightnessContrast(source: Bitmap, brightness: Float, contrast: Float): Bitmap {
        return applyColorAdjustments(source, brightness, contrast, saturation = 1f)
    }

    /**
     * saturation: 0 = 그레이스케일, 1 = 원본, 1보다 크면 채도 증가.
     * brightness / contrast 의미는 [applyBrightnessContrast] 와 동일.
     */
    fun applyColorAdjustments(
        source: Bitmap,
        brightness: Float,
        contrast: Float,
        saturation: Float,
    ): Bitmap {
        val safe = asSoftwareArgb(source)
        val out = Bitmap.createBitmap(safe.width, safe.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cm = ColorMatrix()
        cm.setSaturation(saturation)
        val offset = brightness * 80f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, offset,
                0f, contrast, 0f, 0f, offset,
                0f, 0f, contrast, 0f, offset,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        cm.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(safe, 0f, 0f, paint)
        if (safe !== source) safe.recycle()
        return out
    }
}
