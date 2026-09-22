package com.pocket4cut

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.rendering.SummerFrameVectorDecor
import com.pocket4cut.presentation.detailEdit.DetailEditViewModel
import com.pocket4cut.presentation.navigation.FrameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class BitmapPipelineInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as Application

    @Test
    fun allExifOrientationsPreserveTheExpectedPixelPositions() {
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN)
        val fixture = Bitmap.createBitmap(160, 240, Bitmap.Config.ARGB_8888)
        val file = File.createTempFile("exif-orientations-", ".jpg", app.cacheDir)
        try {
            val canvas = Canvas(fixture)
            colors.forEachIndexed { index, color ->
                val x = (index % 2) * 80f
                val y = (index / 2) * 80f
                canvas.drawRect(x, y, x + 80f, y + 80f, Paint().apply { this.color = color })
            }
            file.outputStream().use { assertTrue(fixture.compress(Bitmap.CompressFormat.JPEG, 100, it)) }
            val expected = listOf(
                listOf(0, 1, 2, 3, 4, 5), // normal
                listOf(1, 0, 3, 2, 5, 4), // mirror horizontally
                listOf(5, 4, 3, 2, 1, 0), // rotate 180
                listOf(4, 5, 2, 3, 0, 1), // mirror vertically
                listOf(0, 2, 4, 1, 3, 5), // transpose
                listOf(4, 2, 0, 5, 3, 1), // rotate 90 clockwise
                listOf(5, 3, 1, 4, 2, 0), // transverse
                listOf(1, 3, 5, 0, 2, 4), // rotate 270 clockwise
            )
            for (orientation in 1..8) {
                ExifInterface(file).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                    saveAttributes()
                }
                val decoded = requireNotNull(BitmapDecoding.decodeSampled(file.absolutePath, 1024))
                try {
                    val columns = if (orientation <= 4) 2 else 3
                    val rows = if (orientation <= 4) 3 else 2
                    assertEquals(columns * 80, decoded.width)
                    assertEquals(rows * 80, decoded.height)
                    expected[orientation - 1].forEachIndexed { index, colorIndex ->
                        val actual = decoded.getPixel(index % columns * 80 + 40, index / columns * 80 + 40)
                        val wanted = colors[colorIndex]
                        assertTrue("EXIF $orientation, tile $index",
                            abs(Color.red(actual) - Color.red(wanted)) <= 4 &&
                                abs(Color.green(actual) - Color.green(wanted)) <= 4 &&
                                abs(Color.blue(actual) - Color.blue(wanted)) <= 4)
                    }
                } finally {
                    decoded.recycle()
                }
            }
        } finally {
            fixture.recycle()
            file.delete()
        }
    }

    @Test
    fun decoderBoundsLargeAndPanoramicSourcesBeforeAllocating() {
        for ((width, height, edge, pixelLimit) in listOf(
            listOf(4000, 3000, 3072, 6_000_000),
            listOf(4000, 3000, 1024, 1_000_000),
            listOf(4096, 64, 256, 6_000_000),
        )) {
            val file = File.createTempFile("decode-budget-", ".jpg", app.cacheDir)
            try {
                val fixture = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    fixture.eraseColor(Color.GRAY)
                    file.outputStream().use { assertTrue(fixture.compress(Bitmap.CompressFormat.JPEG, 92, it)) }
                } finally {
                    fixture.recycle()
                }
                val decoded = requireNotNull(BitmapDecoding.decodeSampled(file.absolutePath, edge, pixelLimit.toLong()))
                try {
                    assertTrue("Long edge exceeded $edge", maxOf(decoded.width, decoded.height) <= edge)
                    assertTrue("Pixel allocation exceeded $pixelLimit", decoded.width.toLong() * decoded.height <= pixelLimit)
                } finally {
                    decoded.recycle()
                }
            } finally {
                file.delete()
            }
        }
    }

    @Test
    fun immediateAndRapidDetailChangesPublishTheLatestAdjustmentForEverySlot() {
        val store = ViewModelStore()
        val sources = List(6) {
            Bitmap.createBitmap(640, 800, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(100, 100, 100)) }
        }
        lateinit var viewModel: DetailEditViewModel
        try {
            instrumentation.runOnMainSync {
                viewModel = ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory(app))[DetailEditViewModel::class.java]
                viewModel.initialize(
                    baseImages = sources,
                    imagePaths = emptyList(),
                    photoIds = List(6) { "photo-$it" },
                    initialAdjustments = emptyMap(),
                    layoutVersion = 2,
                    dateText = "2026.09.22",
                    frameType = FrameType.SIX_CUT,
                    frameStyle = FrameLayouts.byId(FrameLayoutId.SIX_GRID_3X2),
                    theme = FrameCatalog.themes(FrameType.SIX_CUT).first(),
                    frameColor = FrameColors.byId("white"),
                    globalFilter = FilterId.ORIGINAL,
                    customText = "",
                    showDate = false,
                    sessionId = "preview-in-memory-${System.nanoTime()}",
                    selectedIndexes = (0..5).toList(),
                )
                repeat(12) {
                    viewModel.selectSlot(it % 6)
                    viewModel.rotateCurrentSlot()
                    viewModel.setBrightness(0.1f)
                }
                viewModel.resetAllSlots()
                for (slot in 0..5) {
                    viewModel.selectSlot(slot)
                    viewModel.setBrightness(0.35f)
                }
            }
            val deadline = SystemClock.uptimeMillis() + 10_000
            fun hasLatestPixels(): Boolean {
                val images = viewModel.uiState.value.collagePreviewImages
                return images.size == 6 && images.all {
                    it.width == 640 && it.height == 800 && abs(Color.red(it.getPixel(320, 400)) - 128) <= 1
                }
            }
            while (!hasLatestPixels() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(10)
            assertTrue("Latest slot adjustments were replaced by an earlier preview", hasLatestPixels())
            val retained = viewModel.uiState.value.collagePreviewImages
            instrumentation.runOnMainSync { store.clear() }
            retained.forEach { assertTrue("Published preview recycled on clear", !it.isRecycled) }
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            sources.forEach { it.recycle() }
        }
    }

    @Test
    fun parallelSummerRendersMatchIndependentSerialRenders() {
        fun render(adjacent: Boolean): Bitmap = Bitmap.createBitmap(240, 720, Bitmap.Config.ARGB_8888).apply {
            SummerFrameVectorDecor.draw(Canvas(this), width.toFloat(), height.toFloat(), adjacent)
        }
        val expected = listOf(render(false), render(true))
        val executor = Executors.newFixedThreadPool(4)
        try {
            val tasks = (0 until 4).map { worker ->
                executor.submit {
                    repeat(12) { iteration ->
                        val mode = (worker + iteration) % 2
                        val actual = render(mode == 1)
                        try {
                            assertTrue("Summer paint state leaked between renders", expected[mode].sameAs(actual))
                        } finally {
                            actual.recycle()
                        }
                    }
                }
            }
            tasks.forEach { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(10, TimeUnit.SECONDS)
            expected.forEach { it.recycle() }
        }
    }
}
