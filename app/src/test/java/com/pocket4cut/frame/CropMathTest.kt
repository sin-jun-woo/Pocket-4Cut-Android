package com.pocket4cut.frame

import com.pocket4cut.domain.model.PhotoCrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max

class CropMathTest {
    private val epsilon = 0.00001f

    @Test
    fun mapsStoredFocusForEveryQuarterTurnAndHorizontalFlip() {
        val crop = PhotoCrop(focusX = 0.2f, focusY = 0.3f, zoom = 2f)
        val expected = listOf(
            NormalizedPoint(0.2f, 0.3f),
            NormalizedPoint(0.7f, 0.2f),
            NormalizedPoint(0.8f, 0.7f),
            NormalizedPoint(0.3f, 0.8f),
        )
        expected.forEachIndexed { turns, point ->
            assertPoint(point, CropMath.displayFocus(crop, turns, false))
            assertPoint(
                NormalizedPoint(1f - point.x, point.y),
                CropMath.displayFocus(crop, turns, true),
            )
        }
    }

    @Test
    fun displayAndStoredFocusConversionsRoundTripForAllTransforms() {
        val crops = listOf(
            PhotoCrop(0f, 0f, 1f),
            PhotoCrop(0.17f, 0.83f, 2.5f),
            PhotoCrop(1f, 1f, 4f),
        )
        for (crop in crops) for (turns in -4..7) for (flip in listOf(false, true)) {
            val displayed = CropMath.displayFocus(crop, turns, flip)
            val restored = CropMath.inverseDisplayFocus(displayed, turns, flip)
            assertEquals("x for $crop/$turns/$flip", crop.focusX, restored.x, epsilon)
            assertEquals("y for $crop/$turns/$flip", crop.focusY, restored.y, epsilon)
        }
    }

    @Test
    fun neutralCropIsExactlyTheLegacyCenteredAspectFill() {
        val viewport = CropRect(17f, 23f, 317f, 423f)
        val imageWidth = 640f
        val imageHeight = 360f
        val actual = CropMath.drawRect(imageWidth, imageHeight, viewport)

        val legacyScale = max(viewport.width / imageWidth, viewport.height / imageHeight)
        val legacyWidth = imageWidth * legacyScale
        val legacyHeight = imageHeight * legacyScale
        val legacyLeft = viewport.left + (viewport.width - legacyWidth) / 2f
        val legacyTop = viewport.top + (viewport.height - legacyHeight) / 2f

        assertEquals(legacyLeft, actual.left, 0f)
        assertEquals(legacyTop, actual.top, 0f)
        assertEquals(legacyLeft + legacyWidth, actual.right, 0f)
        assertEquals(legacyTop + legacyHeight, actual.bottom, 0f)

        val roundedAspect = CropMath.drawRect(
            imageWidth = 57f,
            imageHeight = 76f,
            viewport = CropRect(0f, 0f, 75f, 100f),
        )
        assertTrue(roundedAspect.left <= 0f && roundedAspect.right >= 75f)
        assertTrue(roundedAspect.top <= 0f && roundedAspect.bottom >= 100f)
    }

    @Test
    fun everySupportedZoomAndTransformCoversViewportWithoutBlankEdges() {
        val imageSizes = listOf(4000f to 500f, 500f to 4000f, 4032f to 3024f, 3024f to 4032f)
        val viewports = listOf(
            CropRect(0f, 0f, 100f, 100f),
            CropRect(20f, 10f, 320f, 410f),
            CropRect(5f, 7f, 605f, 207f),
        )
        val focuses = listOf(0f to 0f, 0.5f to 0.5f, 1f to 1f, 0.05f to 0.95f)
        for ((width, height) in imageSizes) for (viewport in viewports) {
            for ((x, y) in focuses) for (zoom in listOf(1f, 1.01f, 2f, 4f)) {
                for (turns in 0..3) for (flip in listOf(false, true)) {
                    val rect = CropMath.drawRect(
                        imageWidth = width,
                        imageHeight = height,
                        viewport = viewport,
                        transform = PhotoCropTransform(PhotoCrop(x, y, zoom), turns, flip),
                    )
                    assertTrue("left edge $rect must cover $viewport", rect.left <= viewport.left + epsilon)
                    assertTrue("top edge $rect must cover $viewport", rect.top <= viewport.top + epsilon)
                    assertTrue("right edge $rect must cover $viewport", rect.right + epsilon >= viewport.right)
                    assertTrue("bottom edge $rect must cover $viewport", rect.bottom + epsilon >= viewport.bottom)
                }
            }
        }
    }

    @Test
    fun drawRectUsesRotatedThenMirroredFocusOnTransformedBitmap() {
        val crop = PhotoCrop(focusX = 0.2f, focusY = 0.3f, zoom = 2f)
        val viewport = CropRect(0f, 0f, 100f, 100f)
        val rotated = CropMath.drawRect(
            imageWidth = 1000f,
            imageHeight = 500f,
            viewport = viewport,
            transform = PhotoCropTransform(crop, quarterTurnsClockwise = 1, flipHorizontal = false),
        )
        val rotatedAndFlipped = CropMath.drawRect(
            imageWidth = 1000f,
            imageHeight = 500f,
            viewport = viewport,
            transform = PhotoCropTransform(crop, quarterTurnsClockwise = 1, flipHorizontal = true),
        )

        // q1 maps (.2,.3) to (.7,.2); the vertical value clamps to .25 at this geometry.
        assertEquals(-230f, rotated.left, epsilon)
        assertEquals(0f, rotated.top, epsilon)
        // Horizontal mirroring happens after rotation, changing display x from .7 to .3.
        assertEquals(-70f, rotatedAndFlipped.left, epsilon)
        assertEquals(0f, rotatedAndFlipped.top, epsilon)

        val visibleInStoredSource = CropMath.visibleSourceRect(
            imageWidth = 1000f,
            imageHeight = 500f,
            viewport = viewport,
            transform = PhotoCropTransform(crop, quarterTurnsClockwise = 1, flipHorizontal = false),
        )
        assertEquals(0f, visibleInStoredSource.left, epsilon)
        assertEquals(0.175f, visibleInStoredSource.top, epsilon)
        assertEquals(0.5f, visibleInStoredSource.right, epsilon)
        assertEquals(0.425f, visibleInStoredSource.bottom, epsilon)
    }

    @Test
    fun clampMovesOnlyFocusAndKeepsZoom() {
        val source = PhotoCrop(0f, 1f, 3.25f)
        val clamped = CropMath.clampCrop(
            crop = source,
            imageWidth = 1200f,
            imageHeight = 800f,
            viewportWidth = 300f,
            viewportHeight = 400f,
            quarterTurnsClockwise = 1,
            flipHorizontal = true,
        )
        assertEquals(source.zoom, clamped.zoom, 0f)
        assertTrue(clamped.focusX in 0f..1f)
        assertTrue(clamped.focusY in 0f..1f)
        assertFalse(clamped == source)

        val gestureOvershoot = CropMath.clampCrop(
            crop = PhotoCrop(-0.25f, 1.3f, 2f),
            imageWidth = 1200f,
            imageHeight = 800f,
            viewportWidth = 300f,
            viewportHeight = 400f,
        )
        assertTrue(gestureOvershoot.focusX in 0f..1f)
        assertTrue(gestureOvershoot.focusY in 0f..1f)
        assertEquals(2f, gestureOvershoot.zoom, 0f)
    }

    @Test
    fun rejectsInvalidCropAndDimensionsInsteadOfSilentlyChangingThem() {
        listOf(
            PhotoCrop(Float.NaN, 0.5f, 1f),
            PhotoCrop(0.5f, Float.POSITIVE_INFINITY, 1f),
            PhotoCrop(-0.01f, 0.5f, 1f),
            PhotoCrop(0.5f, 1.01f, 1f),
            PhotoCrop(0.5f, 0.5f, 0.99f),
            PhotoCrop(0.5f, 0.5f, 4.01f),
        ).forEach { crop ->
            assertFalse(CropMath.isValid(crop))
            assertThrows(IllegalArgumentException::class.java) { CropMath.requireValid(crop) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            CropMath.drawRect(0f, 100f, CropRect(0f, 0f, 100f, 100f))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CropMath.drawRect(100f, 100f, CropRect(0f, 0f, Float.NaN, 100f))
        }
    }

    private fun assertPoint(expected: NormalizedPoint, actual: NormalizedPoint) {
        assertEquals(expected.x, actual.x, epsilon)
        assertEquals(expected.y, actual.y, epsilon)
    }
}
