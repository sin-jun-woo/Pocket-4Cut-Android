package com.pocket4cut.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object BitmapDecoding {
    data class ImageDimensions(val width: Int, val height: Int)

    data class NormalizedImageRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
    }

    private data class NormalizedPoint(val x: Float, val y: Float)

    /** Returns full source bounds after accounting for the stored EXIF orientation. */
    fun orientedDimensions(path: String): ImageDimensions? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        return when (readExifOrientation(path)) {
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270,
            -> ImageDimensions(options.outHeight, options.outWidth)
            else -> ImageDimensions(options.outWidth, options.outHeight)
        }
    }

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

    /**
     * Region-decodes an exact rectangle in EXIF-normalized coordinates. Only JPEG, PNG and static
     * WebP use this path; unsupported formats and decoder failures return null for a safe full-image
     * fallback. The returned bitmap is already EXIF-normalized and never exceeds the supplied limits.
     */
    @Suppress("DEPRECATION")
    fun decodeOrientedCropSampled(
        path: String,
        orientedCrop: NormalizedImageRect,
        maxLongEdge: Int,
        maxPixels: Long = 6_000_000L,
    ): Bitmap? {
        require(maxLongEdge > 0 && maxPixels > 0)
        require(isValidNormalizedRect(orientedCrop)) { "Crop rectangle must be finite and inside 0..1" }
        if (!supportsRegionDecode(path)) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val rawWidth = bounds.outWidth
        val rawHeight = bounds.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null
        val orientation = readExifOrientation(path)

        val rawNormalized = mapRect(orientedCrop) { orientedToRaw(it, orientation) }
        val rawRect = Rect(
            floor(rawNormalized.left * rawWidth).toInt().coerceIn(0, rawWidth - 1),
            floor(rawNormalized.top * rawHeight).toInt().coerceIn(0, rawHeight - 1),
            ceil(rawNormalized.right * rawWidth).toInt().coerceIn(1, rawWidth),
            ceil(rawNormalized.bottom * rawHeight).toInt().coerceIn(1, rawHeight),
        )
        if (rawRect.right <= rawRect.left || rawRect.bottom <= rawRect.top) return null

        val sample = calculateInSampleSize(
            width = rawRect.width(),
            height = rawRect.height(),
            maxLongEdge = maxLongEdge,
            maxPixels = maxPixels,
        )
        val decoder = try {
            BitmapRegionDecoder.newInstance(path, false)
        } catch (error: OutOfMemoryError) {
            throw error
        } catch (_: Exception) {
            null
        } ?: return null
        var orientedRegion: Bitmap? = null
        return try {
            val decoded = decoder.decodeRegion(
                rawRect,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                },
            ) ?: return null
            val normalizedRegion = applyExifOrientation(decoded, orientation)
            orientedRegion = normalizedRegion

            val actualRawRegion = NormalizedImageRect(
                rawRect.left.toFloat() / rawWidth,
                rawRect.top.toFloat() / rawHeight,
                rawRect.right.toFloat() / rawWidth,
                rawRect.bottom.toFloat() / rawHeight,
            )
            val actualOrientedRegion = mapRect(actualRawRegion) { rawToOriented(it, orientation) }
            val sourceRect = RectF(
                ((orientedCrop.left - actualOrientedRegion.left) / actualOrientedRegion.width * normalizedRegion.width)
                    .coerceIn(0f, normalizedRegion.width.toFloat()),
                ((orientedCrop.top - actualOrientedRegion.top) / actualOrientedRegion.height * normalizedRegion.height)
                    .coerceIn(0f, normalizedRegion.height.toFloat()),
                ((orientedCrop.right - actualOrientedRegion.left) / actualOrientedRegion.width * normalizedRegion.width)
                    .coerceIn(0f, normalizedRegion.width.toFloat()),
                ((orientedCrop.bottom - actualOrientedRegion.top) / actualOrientedRegion.height * normalizedRegion.height)
                    .coerceIn(0f, normalizedRegion.height.toFloat()),
            )
            if (sourceRect.width() <= 0f || sourceRect.height() <= 0f) return null
            val targetSize = boundedSize(
                width = sourceRect.width().roundToInt().coerceAtLeast(1),
                height = sourceRect.height().roundToInt().coerceAtLeast(1),
                maxLongEdge = maxLongEdge,
                maxPixels = maxPixels,
            )
            val cropped = createBitmap(targetSize.width, targetSize.height)
            try {
                val cropMatrix = Matrix().apply {
                    setRectToRect(
                        sourceRect,
                        RectF(0f, 0f, cropped.width.toFloat(), cropped.height.toFloat()),
                        Matrix.ScaleToFit.FILL,
                    )
                }
                Canvas(cropped).drawBitmap(
                    normalizedRegion,
                    cropMatrix,
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
                )
                cropped
            } catch (cause: Throwable) {
                cropped.recycle()
                throw cause
            }
        } catch (error: OutOfMemoryError) {
            throw error
        } catch (_: Exception) {
            null
        } finally {
            orientedRegion?.let { if (!it.isRecycled) it.recycle() }
            decoder.recycle()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, maxLongEdge: Int, maxPixels: Long): Int {
        return calculateInSampleSize(options.outWidth, options.outHeight, maxLongEdge, maxPixels)
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxLongEdge: Int,
        maxPixels: Long,
    ): Int {
        var sample = 1
        while (true) {
            // Round up so codecs that round odd source dimensions up still fit the budget.
            val sampledWidth = (width.toLong() + sample - 1) / sample
            val sampledHeight = (height.toLong() + sample - 1) / sample
            if (
                max(sampledWidth, sampledHeight) <= maxLongEdge &&
                sampledWidth * sampledHeight <= maxPixels
            ) return sample
            sample *= 2
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, path: String): Bitmap =
        applyExifOrientation(bitmap, readExifOrientation(path))

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {

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

    private fun readExifOrientation(path: String): Int = runCatching {
        ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun isValidNormalizedRect(rect: NormalizedImageRect): Boolean =
        rect.left.isFinite() && rect.top.isFinite() && rect.right.isFinite() && rect.bottom.isFinite() &&
            rect.left >= 0f && rect.left < rect.right && rect.right <= 1f &&
            rect.top >= 0f && rect.top < rect.bottom && rect.bottom <= 1f

    private fun mapRect(
        rect: NormalizedImageRect,
        transform: (NormalizedPoint) -> NormalizedPoint,
    ): NormalizedImageRect {
        val points = listOf(
            NormalizedPoint(rect.left, rect.top),
            NormalizedPoint(rect.right, rect.top),
            NormalizedPoint(rect.right, rect.bottom),
            NormalizedPoint(rect.left, rect.bottom),
        ).map(transform)
        return NormalizedImageRect(
            points.minOf { it.x }.coerceIn(0f, 1f),
            points.minOf { it.y }.coerceIn(0f, 1f),
            points.maxOf { it.x }.coerceIn(0f, 1f),
            points.maxOf { it.y }.coerceIn(0f, 1f),
        )
    }

    /** Raw encoded coordinates to EXIF-normalized display coordinates. */
    private fun rawToOriented(point: NormalizedPoint, orientation: Int): NormalizedPoint = when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> NormalizedPoint(1f - point.x, point.y)
        ExifInterface.ORIENTATION_ROTATE_180 -> NormalizedPoint(1f - point.x, 1f - point.y)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> NormalizedPoint(point.x, 1f - point.y)
        ExifInterface.ORIENTATION_TRANSPOSE -> NormalizedPoint(point.y, point.x)
        ExifInterface.ORIENTATION_ROTATE_90 -> NormalizedPoint(1f - point.y, point.x)
        ExifInterface.ORIENTATION_TRANSVERSE -> NormalizedPoint(1f - point.y, 1f - point.x)
        ExifInterface.ORIENTATION_ROTATE_270 -> NormalizedPoint(point.y, 1f - point.x)
        else -> point
    }

    /** EXIF-normalized display coordinates back to the raw encoded image. */
    private fun orientedToRaw(point: NormalizedPoint, orientation: Int): NormalizedPoint = when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> NormalizedPoint(1f - point.x, point.y)
        ExifInterface.ORIENTATION_ROTATE_180 -> NormalizedPoint(1f - point.x, 1f - point.y)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> NormalizedPoint(point.x, 1f - point.y)
        ExifInterface.ORIENTATION_TRANSPOSE -> NormalizedPoint(point.y, point.x)
        ExifInterface.ORIENTATION_ROTATE_90 -> NormalizedPoint(point.y, 1f - point.x)
        ExifInterface.ORIENTATION_TRANSVERSE -> NormalizedPoint(1f - point.y, 1f - point.x)
        ExifInterface.ORIENTATION_ROTATE_270 -> NormalizedPoint(1f - point.y, point.x)
        else -> point
    }

    private fun supportsRegionDecode(path: String): Boolean = runCatching {
        val header = ByteArray(32)
        val count = FileInputStream(path).use { it.read(header) }
        if (count >= 3 && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
            return@runCatching true
        }
        if (
            count >= 8 && header.copyOfRange(0, 8).contentEquals(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
            )
        ) return@runCatching true
        val isWebP = count >= 16 &&
            String(header, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(header, 8, 4, Charsets.US_ASCII) == "WEBP"
        if (!isWebP) return@runCatching false
        val chunk = String(header, 12, 4, Charsets.US_ASCII)
        when (chunk) {
            "VP8 ", "VP8L" -> true
            "VP8X" -> count > 20 && (header[20].toInt() and 0x02) == 0
            else -> false
        }
    }.getOrDefault(false)

    private fun boundedSize(
        width: Int,
        height: Int,
        maxLongEdge: Int,
        maxPixels: Long,
    ): ImageDimensions {
        val longEdgeScale = maxLongEdge.toDouble() / max(width, height)
        val pixelScale = sqrt(maxPixels.toDouble() / (width.toLong() * height).toDouble())
        val scale = min(1.0, min(longEdgeScale, pixelScale))
        return ImageDimensions(
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
        )
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

