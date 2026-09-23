package com.pocket4cut

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.exifinterface.media.ExifInterface
import com.pocket4cut.core.util.BitmapAdjustments
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.domain.model.PhotoCrop
import com.pocket4cut.frame.CollageLayoutMath
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.frame.CropMath
import com.pocket4cut.frame.CropRect
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.PhotoCropTransform
import com.pocket4cut.presentation.navigation.FrameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/** Synthetic-only crop checks; no session, MediaStore, or user files are opened. */
@RunWith(AndroidJUnit4::class)
class CropRendererInstrumentedTest {
    @Test
    fun explicitNeutralCropIsPixelIdenticalToLegacyDefaultForEveryLayout() {
        val photos = (0 until 6).map { index -> directionalFixture(index) }
        try {
            for (style in FrameLayouts.all) {
                val type = FrameType.entries.first { it.selectCount == style.slots }
                val theme = FrameCatalog.themes(type).first()
                val layout = CollageLayoutMath.computeForPreview(style, theme, null, 390f)
                val defaultResult = bitmap(layout.canvasWidth, layout.canvasHeight)
                val neutralResult = bitmap(layout.canvasWidth, layout.canvasHeight)
                try {
                    CollageRenderer.drawScene(
                        Canvas(defaultResult),
                        CollageRenderer.Input(
                            images = photos.take(style.slots),
                            frameStyle = style,
                            theme = theme,
                        ),
                        layout,
                    )
                    CollageRenderer.drawScene(
                        Canvas(neutralResult),
                        CollageRenderer.Input(
                            images = photos.take(style.slots),
                            cropTransforms = List(style.slots) { PhotoCropTransform() },
                            frameStyle = style,
                            theme = theme,
                        ),
                        layout,
                    )
                    assertTrue("${style.id} neutral crop changed legacy pixels", defaultResult.sameAs(neutralResult))
                } finally {
                    defaultResult.recycle()
                    neutralResult.recycle()
                }
            }
        } finally {
            photos.forEach(Bitmap::recycle)
        }
    }

    @Test
    fun cropDataIsAppliedBySlotOrderAndNeverExposesBlankPixels() {
        val style = FrameLayouts.all.first { it.slots == 2 }
        val theme = FrameCatalog.themes(FrameType.TWO_CUT).first()
        val layout = CollageLayoutMath.computeForPreview(style, theme, null, 390f)
        val fixture = horizontalBands()
        val output = bitmap(layout.canvasWidth, layout.canvasHeight)
        try {
            CollageRenderer.drawScene(
                Canvas(output),
                CollageRenderer.Input(
                    images = listOf(fixture, fixture),
                    cropTransforms = listOf(
                        PhotoCropTransform(PhotoCrop(focusX = 0.15f, focusY = 0.5f, zoom = 2f)),
                        PhotoCropTransform(PhotoCrop(focusX = 0.85f, focusY = 0.5f, zoom = 2f)),
                    ),
                    frameStyle = style,
                    theme = theme,
                ),
                layout,
            )

            val firstCenter = layout.cells[0].let { output.getPixel(it.centerX().toInt(), it.centerY().toInt()) }
            val secondCenter = layout.cells[1].let { output.getPixel(it.centerX().toInt(), it.centerY().toInt()) }
            assertEquals(Color.RED, firstCenter)
            assertEquals(Color.BLUE, secondCenter)
            assertFalse("Slot crops must be independent", firstCenter == secondCenter)
            layout.cells.forEachIndexed { index, slot ->
                val expected = if (index == 0) Color.RED else Color.BLUE
                for (x in listOf(slot.left + 1f, slot.centerX(), slot.right - 1f)) {
                    for (y in listOf(slot.top + 1f, slot.centerY(), slot.bottom - 1f)) {
                        assertEquals(
                            "Crop exposed a slot edge instead of the selected source region",
                            expected,
                            output.getPixel(x.toInt(), y.toInt()),
                        )
                    }
                }
            }
        } finally {
            fixture.recycle()
            output.recycle()
        }
    }

    @Test
    fun regionDecodedExportMatchesFullSourceCropAfterExifRotationAndUserTransform() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
        val file = File.createTempFile("crop-region-export-", ".jpg", context.cacheDir)
        var full: Bitmap? = null
        var region: Bitmap? = null
        var fullTransformed: Bitmap? = null
        var regionTransformed: Bitmap? = null
        val filler = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
        try {
            for (y in 0 until source.height) for (x in 0 until source.width) {
                source.setPixel(
                    x,
                    y,
                    Color.rgb(x * 255 / (source.width - 1), y * 255 / (source.height - 1), (x + y) % 256),
                )
            }
            file.outputStream().use { assertTrue(source.compress(Bitmap.CompressFormat.JPEG, 100, it)) }
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }

            val style = FrameLayouts.all.first { it.slots == 2 }
            val theme = FrameCatalog.themes(FrameType.TWO_CUT).first()
            val layout = CollageLayoutMath.computeForPreview(style, theme, null, 390f)
            val slot = layout.cells.first()
            val transform = PhotoCropTransform(
                crop = PhotoCrop(focusX = 0.72f, focusY = 0.35f, zoom = 2.3f),
                quarterTurnsClockwise = 1,
                flipHorizontal = true,
            )
            full = requireNotNull(BitmapDecoding.decodeSampled(file.absolutePath, 4096))
            val displayedWidth = full.height.toFloat()
            val displayedHeight = full.width.toFloat()
            val visible = CropMath.visibleSourceRect(
                displayedWidth,
                displayedHeight,
                CropRect(0f, 0f, slot.width(), slot.height()),
                transform,
            )
            region = requireNotNull(BitmapDecoding.decodeOrientedCropSampled(
                file.absolutePath,
                BitmapDecoding.NormalizedImageRect(visible.left, visible.top, visible.right, visible.bottom),
                maxLongEdge = 4096,
            ))
            fullTransformed = applyUserTransform(requireNotNull(full), 1, true)
            full = null
            regionTransformed = applyUserTransform(requireNotNull(region), 1, true)
            region = null
            val fullReady = requireNotNull(fullTransformed)
            val regionReady = requireNotNull(regionTransformed)

            val baseline = bitmap(layout.canvasWidth, layout.canvasHeight)
            val optimized = bitmap(layout.canvasWidth, layout.canvasHeight)
            try {
                CollageRenderer.drawScene(
                    Canvas(baseline),
                    CollageRenderer.Input(
                        images = listOf(fullReady, filler),
                        cropTransforms = listOf(transform, PhotoCropTransform()),
                        frameStyle = style,
                        theme = theme,
                    ),
                    layout,
                )
                CollageRenderer.drawScene(
                    Canvas(optimized),
                    CollageRenderer.Input(
                        images = emptyList(),
                        slotImageProvider = { index, width, height ->
                            assertEquals(layout.cells[index].width().roundToInt(), width)
                            assertEquals(layout.cells[index].height().roundToInt(), height)
                            CollageRenderer.ProvidedSlotImage(
                                if (index == 0) regionReady else filler,
                                PhotoCropTransform(),
                            )
                        },
                        frameStyle = style,
                        theme = theme,
                    ),
                    layout,
                )

                var delta = 0L
                var samples = 0
                for (y in slot.top.toInt() + 3 until slot.bottom.toInt() - 3 step 2) {
                    for (x in slot.left.toInt() + 3 until slot.right.toInt() - 3 step 2) {
                        val a = baseline.getPixel(x, y)
                        val b = optimized.getPixel(x, y)
                        delta += abs(Color.red(a) - Color.red(b)) +
                            abs(Color.green(a) - Color.green(b)) + abs(Color.blue(a) - Color.blue(b))
                        samples++
                    }
                }
                assertTrue("Region export mean RGB delta=${delta.toDouble() / (samples * 3)}",
                    delta.toDouble() / (samples * 3) < 5.0)
            } finally {
                baseline.recycle()
                optimized.recycle()
            }
        } finally {
            source.recycle()
            filler.recycle()
            full?.recycle()
            region?.recycle()
            fullTransformed?.recycle()
            regionTransformed?.recycle()
            file.delete()
        }
    }

    @Test
    fun allExifOrientationsQuarterTurnsAndMirrorsRetainTheSameVisibleCrop() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(160, 240, Bitmap.Config.ARGB_8888)
        val file = File.createTempFile("crop-transform-matrix-", ".jpg", context.cacheDir)
        try {
            for (y in 0 until source.height) for (x in 0 until source.width) {
                source.setPixel(
                    x,
                    y,
                    Color.rgb(x * 255 / 159, y * 255 / 239, (x * 3 + y * 2) % 256),
                )
            }
            file.outputStream().use { assertTrue(source.compress(Bitmap.CompressFormat.JPEG, 100, it)) }
            val viewport = CropRect(0f, 0f, 75f, 100f)
            for (orientation in 1..8) {
                ExifInterface(file).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                    saveAttributes()
                }
                val full = requireNotNull(BitmapDecoding.decodeSampled(file.absolutePath, 1024))
                try {
                    for (turns in 0..3) for (flip in listOf(false, true)) {
                        val transform = PhotoCropTransform(
                            PhotoCrop(focusX = 0.63f, focusY = 0.42f, zoom = 2.2f),
                            turns,
                            flip,
                        )
                        val displayedWidth = if (turns % 2 == 0) full.width else full.height
                        val displayedHeight = if (turns % 2 == 0) full.height else full.width
                        val visible = CropMath.visibleSourceRect(
                            displayedWidth.toFloat(),
                            displayedHeight.toFloat(),
                            viewport,
                            transform,
                        )
                        val region = requireNotNull(BitmapDecoding.decodeOrientedCropSampled(
                            file.absolutePath,
                            BitmapDecoding.NormalizedImageRect(
                                visible.left, visible.top, visible.right, visible.bottom,
                            ),
                            maxLongEdge = 1024,
                        ))
                        val fullUser = applyUserTransform(
                            full.copy(Bitmap.Config.ARGB_8888, false), turns, flip,
                        )
                        val regionUser = applyUserTransform(region, turns, flip)
                        val baseline = renderViewport(fullUser, viewport, transform)
                        val optimized = renderViewport(regionUser, viewport, PhotoCropTransform())
                        try {
                            var delta = 0L
                            var samples = 0
                            for (y in 2 until optimized.height - 2 step 3) {
                                for (x in 2 until optimized.width - 2 step 3) {
                                    val a = baseline.getPixel(x, y)
                                    val b = optimized.getPixel(x, y)
                                    delta += abs(Color.red(a) - Color.red(b)) +
                                        abs(Color.green(a) - Color.green(b)) +
                                        abs(Color.blue(a) - Color.blue(b))
                                    samples++
                                }
                            }
                            val mean = delta.toDouble() / (samples * 3)
                            assertTrue("EXIF $orientation/q$turns/flip=$flip delta=$mean", mean < 8.0)
                        } finally {
                            baseline.recycle()
                            optimized.recycle()
                            fullUser.recycle()
                            regionUser.recycle()
                        }
                    }
                } finally {
                    full.recycle()
                }
            }
        } finally {
            source.recycle()
            file.delete()
        }
    }

    private fun horizontalBands(): Bitmap = Bitmap.createBitmap(900, 300, Bitmap.Config.ARGB_8888).apply {
        val canvas = Canvas(this)
        canvas.drawRect(0f, 0f, 300f, height.toFloat(), Paint().apply { color = Color.RED })
        canvas.drawRect(300f, 0f, 600f, height.toFloat(), Paint().apply { color = Color.GREEN })
        canvas.drawRect(600f, 0f, 900f, height.toFloat(), Paint().apply { color = Color.BLUE })
    }

    private fun directionalFixture(seed: Int): Bitmap =
        Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            canvas.drawColor(Color.rgb(30 + seed * 20, 50 + seed * 12, 80 + seed * 10))
            canvas.drawRect(0f, 0f, 53f, height.toFloat(), Paint().apply { color = Color.RED })
            canvas.drawRect(107f, 0f, width.toFloat(), height.toFloat(), Paint().apply { color = Color.BLUE })
        }

    private fun bitmap(width: Float, height: Float): Bitmap = Bitmap.createBitmap(
        width.roundToInt(), height.roundToInt(), Bitmap.Config.ARGB_8888,
    )

    private fun applyUserTransform(source: Bitmap, turns: Int, flip: Boolean): Bitmap {
        var current = source
        repeat(turns) {
            val next = BitmapAdjustments.rotate90(current)
            if (next !== current) current.recycle()
            current = next
        }
        if (flip) {
            val next = BitmapAdjustments.flipHorizontal(current)
            if (next !== current) current.recycle()
            current = next
        }
        return current
    }

    private fun renderViewport(
        source: Bitmap,
        viewport: CropRect,
        transform: PhotoCropTransform,
    ): Bitmap = Bitmap.createBitmap(
        viewport.width.roundToInt(), viewport.height.roundToInt(), Bitmap.Config.ARGB_8888,
    ).apply {
        val target = CropMath.drawRect(
            source.width.toFloat(), source.height.toFloat(), viewport, transform,
        )
        Canvas(this).apply {
            clipRect(0f, 0f, viewport.width, viewport.height)
            drawBitmap(
                source,
                null,
                RectF(target.left, target.top, target.right, target.bottom),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )
        }
    }
}
