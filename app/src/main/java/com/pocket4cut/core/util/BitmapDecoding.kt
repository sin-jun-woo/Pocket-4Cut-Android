package com.pocket4cut.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapDecoding {
    fun decodeSampled(path: String, reqSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val decoded = BitmapFactory.decodeFile(path, options) ?: return null
        return applyExifOrientation(decoded, path)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
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
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(-90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(-90f)
            else -> return bitmap
        }

        val rotated = runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrElse { bitmap }

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

