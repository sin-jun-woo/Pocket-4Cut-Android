package com.pocket4cut.frame

import com.pocket4cut.domain.model.PhotoCrop
import kotlin.math.max

/**
 * Immutable crop input for a bitmap that has already had the user's rotation and mirror applied.
 * [crop] remains stored in the EXIF-normalized, pre-user-transform coordinate space.
 */
data class PhotoCropTransform(
    val crop: PhotoCrop = PhotoCrop(),
    val quarterTurnsClockwise: Int = 0,
    val flipHorizontal: Boolean = false,
)

data class NormalizedPoint(val x: Float, val y: Float)

/** Platform-independent rectangle used by both the editor and Android Canvas renderer. */
data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/** Shared focal-point, pan, zoom and aspect-fill calculations for preview and export. */
object CropMath {
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 4f

    fun isValid(crop: PhotoCrop): Boolean =
        crop.focusX.isFinite() && crop.focusX in 0f..1f &&
            crop.focusY.isFinite() && crop.focusY in 0f..1f &&
            crop.zoom.isFinite() && crop.zoom in MIN_ZOOM..MAX_ZOOM

    fun requireValid(crop: PhotoCrop): PhotoCrop = crop.also {
        require(isValid(it)) {
            "Crop must contain finite focus values in 0..1 and zoom in $MIN_ZOOM..$MAX_ZOOM"
        }
    }

    /** Maps the persisted pre-rotation focal point into the already transformed bitmap. */
    fun displayFocus(
        crop: PhotoCrop,
        quarterTurnsClockwise: Int,
        flipHorizontal: Boolean,
    ): NormalizedPoint {
        requireValid(crop)
        val rotated = when (normalizedQuarterTurns(quarterTurnsClockwise)) {
            0 -> NormalizedPoint(crop.focusX, crop.focusY)
            1 -> NormalizedPoint(1f - crop.focusY, crop.focusX)
            2 -> NormalizedPoint(1f - crop.focusX, 1f - crop.focusY)
            else -> NormalizedPoint(crop.focusY, 1f - crop.focusX)
        }
        return if (flipHorizontal) NormalizedPoint(1f - rotated.x, rotated.y) else rotated
    }

    /** Inverse of [displayFocus], used when editor gestures operate on the transformed bitmap. */
    fun inverseDisplayFocus(
        displayPoint: NormalizedPoint,
        quarterTurnsClockwise: Int,
        flipHorizontal: Boolean,
    ): NormalizedPoint {
        require(displayPoint.x.isFinite() && displayPoint.y.isFinite()) {
            "Display focal point must be finite"
        }
        val x = if (flipHorizontal) 1f - displayPoint.x else displayPoint.x
        val y = displayPoint.y
        return when (normalizedQuarterTurns(quarterTurnsClockwise)) {
            0 -> NormalizedPoint(x, y)
            1 -> NormalizedPoint(y, 1f - x)
            2 -> NormalizedPoint(1f - x, 1f - y)
            else -> NormalizedPoint(1f - y, x)
        }
    }

    /**
     * Clamps a crop so the focal point can remain exactly at the viewport center without exposing
     * a blank edge. [imageWidth] and [imageHeight] describe the bitmap after user rotation/flip.
     */
    fun clampCrop(
        crop: PhotoCrop,
        imageWidth: Float,
        imageHeight: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        quarterTurnsClockwise: Int = 0,
        flipHorizontal: Boolean = false,
    ): PhotoCrop {
        requirePositiveDimensions(imageWidth, imageHeight, viewportWidth, viewportHeight)
        require(crop.focusX.isFinite() && crop.focusY.isFinite()) {
            "Crop focus values must be finite"
        }
        require(crop.zoom.isFinite() && crop.zoom in MIN_ZOOM..MAX_ZOOM) {
            "Crop zoom must be finite and in $MIN_ZOOM..$MAX_ZOOM"
        }
        // Gesture deltas may briefly move beyond normalized bounds. Clamp those candidates here;
        // persisted/render inputs remain strict through requireValid().
        val bounded = crop.copy(
            focusX = crop.focusX.coerceIn(0f, 1f),
            focusY = crop.focusY.coerceIn(0f, 1f),
        )
        val scale = aspectFillScale(imageWidth, imageHeight, viewportWidth, viewportHeight) * bounded.zoom
        val scaledWidth = max(imageWidth * scale, viewportWidth)
        val scaledHeight = max(imageHeight * scale, viewportHeight)
        val minX = (viewportWidth / (2f * scaledWidth)).coerceIn(0f, 0.5f)
        val minY = (viewportHeight / (2f * scaledHeight)).coerceIn(0f, 0.5f)
        val displayed = displayFocus(bounded, quarterTurnsClockwise, flipHorizontal)
        val clampedDisplayed = NormalizedPoint(
            displayed.x.coerceIn(minX, 1f - minX),
            displayed.y.coerceIn(minY, 1f - minY),
        )
        val stored = inverseDisplayFocus(clampedDisplayed, quarterTurnsClockwise, flipHorizontal)
        return bounded.copy(
            focusX = stored.x.coerceIn(0f, 1f),
            focusY = stored.y.coerceIn(0f, 1f),
        )
    }

    /**
     * Destination rectangle for drawing the transformed bitmap through [viewport]. The rectangle
     * always covers the viewport, and zoom=1 with a centered focus is the legacy center aspect-fill.
     */
    fun drawRect(
        imageWidth: Float,
        imageHeight: Float,
        viewport: CropRect,
        transform: PhotoCropTransform = PhotoCropTransform(),
    ): CropRect {
        requirePositiveDimensions(imageWidth, imageHeight, viewport.width, viewport.height)
        requireValid(transform.crop)
        val crop = clampCrop(
            crop = transform.crop,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            viewportWidth = viewport.width,
            viewportHeight = viewport.height,
            quarterTurnsClockwise = transform.quarterTurnsClockwise,
            flipHorizontal = transform.flipHorizontal,
        )
        val focus = displayFocus(crop, transform.quarterTurnsClockwise, transform.flipHorizontal)
        val scale = aspectFillScale(
            imageWidth, imageHeight, viewport.width, viewport.height,
        ) * crop.zoom
        // Float rounding can leave an aspect-identical image a few ULPs smaller than the viewport.
        // Snap each dimension up to the viewport before deriving the legal translation interval.
        val scaledWidth = max(imageWidth * scale, viewport.width)
        val scaledHeight = max(imageHeight * scale, viewport.height)
        // Preserve the legacy operation order for the neutral crop, avoiding even a one-ULP
        // destination change that could alter filtered edge pixels.
        val isLegacyCenter = crop.zoom == MIN_ZOOM && focus.x == 0.5f && focus.y == 0.5f
        val desiredLeft = if (isLegacyCenter) {
            viewport.left + (viewport.width - scaledWidth) / 2f
        } else {
            viewport.centerX - focus.x * scaledWidth
        }
        val desiredTop = if (isLegacyCenter) {
            viewport.top + (viewport.height - scaledHeight) / 2f
        } else {
            viewport.centerY - focus.y * scaledHeight
        }
        val left = desiredLeft.coerceIn(viewport.right - scaledWidth, viewport.left)
        val top = desiredTop.coerceIn(viewport.bottom - scaledHeight, viewport.top)
        return CropRect(left, top, left + scaledWidth, top + scaledHeight)
    }

    /**
     * Returns the visible normalized rectangle in the EXIF-normalized, pre-user-transform source.
     * The supplied image dimensions describe the bitmap after the quarter-turn rotation.
     */
    fun visibleSourceRect(
        imageWidth: Float,
        imageHeight: Float,
        viewport: CropRect,
        transform: PhotoCropTransform,
    ): CropRect {
        val drawn = drawRect(imageWidth, imageHeight, viewport, transform)
        val displayRect = CropRect(
            left = ((viewport.left - drawn.left) / drawn.width).coerceIn(0f, 1f),
            top = ((viewport.top - drawn.top) / drawn.height).coerceIn(0f, 1f),
            right = ((viewport.right - drawn.left) / drawn.width).coerceIn(0f, 1f),
            bottom = ((viewport.bottom - drawn.top) / drawn.height).coerceIn(0f, 1f),
        )
        val sourceCorners = listOf(
            NormalizedPoint(displayRect.left, displayRect.top),
            NormalizedPoint(displayRect.right, displayRect.top),
            NormalizedPoint(displayRect.right, displayRect.bottom),
            NormalizedPoint(displayRect.left, displayRect.bottom),
        ).map { point ->
            inverseDisplayFocus(point, transform.quarterTurnsClockwise, transform.flipHorizontal)
        }
        return CropRect(
            left = sourceCorners.minOf { it.x }.coerceIn(0f, 1f),
            top = sourceCorners.minOf { it.y }.coerceIn(0f, 1f),
            right = sourceCorners.maxOf { it.x }.coerceIn(0f, 1f),
            bottom = sourceCorners.maxOf { it.y }.coerceIn(0f, 1f),
        )
    }

    fun aspectFillScale(
        imageWidth: Float,
        imageHeight: Float,
        viewportWidth: Float,
        viewportHeight: Float,
    ): Float {
        requirePositiveDimensions(imageWidth, imageHeight, viewportWidth, viewportHeight)
        return max(viewportWidth / imageWidth, viewportHeight / imageHeight)
    }

    private fun normalizedQuarterTurns(turns: Int): Int = ((turns % 4) + 4) % 4

    private fun requirePositiveDimensions(vararg dimensions: Float) {
        require(dimensions.all { it.isFinite() && it > 0f }) {
            "Image and viewport dimensions must be finite and positive"
        }
    }
}
