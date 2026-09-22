package com.pocket4cut.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapDecoding {
    /** Decode within fixed pixel/long-edge limits before allocating, including panoramic sources. */
    fun decodeSampled(path: String, reqSize: Int, maxPixels: Long = 6_000_000L): Bitmap? {
        require(reqSize > 0 && maxPixels > 0)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        options.inSampleSize = calculateInSampleSize(options, reqSize, maxPixels)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val decoded = BitmapFactory.decodeFile(path, options) ?: return null
        return applyExifOrientation(decoded, path)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, maxLongEdge: Int, maxPixels: Long): Int {
        var sample = 1
        while (true) {
            // Round up so codecs that round odd source dimensions up still fit the budget.
            val width = (options.outWidth.toLong() + sample - 1) / sample
            val height = (options.outHeight.toLong() + sample - 1) / sample
            if (max(width, height) <= maxLongEdge && width * height <= maxPixels) return sample
            sample *= 2
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, path: String): Bitmap {
        val orientation = runCatching {
            ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.postRotate(180f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setValues(floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f))
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setValues(floatArrayOf(0f, -1f, 0f, -1f, 0f, 0f, 0f, 0f, 1f))
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(-90f)
            else -> return bitmap
        }

        val rotated = try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (cause: Throwable) {
            bitmap.recycle()
            throw cause
        }

        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    /**
     * JPEG 파일을 로드해 long edge가 [maxLongEdge]를 넘지 않도록 스케일한 뒤 같은 경로에 덮어쓴다.
     */
    fun rewriteJpegMaxLongEdge(path: String, maxLongEdge: Int, jpegQuality: Int): Boolean {
        val sampled = decodeSampled(path, maxLongEdge) ?: return false
        val w = sampled.width
        val h = sampled.height
        val longEdge = max(w, h)
        val toWrite = if (longEdge <= maxLongEdge) {
            sampled
        } else {
            val scale = maxLongEdge.toFloat() / longEdge
            val nw = (w * scale).roundToInt().coerceAtLeast(1)
            val nh = (h * scale).roundToInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(sampled, nw, nh, true)
            if (scaled !== sampled) sampled.recycle()
            scaled
        }
        return try {
            FileOutputStream(path).use { out ->
                toWrite.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            }
            true
        } catch (_: Throwable) {
            false
        } finally {
            toWrite.recycle()
        }
    }
}

