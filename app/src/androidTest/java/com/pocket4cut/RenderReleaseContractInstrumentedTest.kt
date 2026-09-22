package com.pocket4cut

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Debug
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.frame.CollageLayoutMath
import com.pocket4cut.frame.CollageOutputSize
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.frame.CustomFrameDecoration
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.NormPoint
import com.pocket4cut.frame.StickerPalette
import com.pocket4cut.presentation.navigation.FrameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.roundToInt

/** Synthetic-only render checks; no session, MediaStore, or user files are opened. */
@RunWith(AndroidJUnit4::class)
class RenderReleaseContractInstrumentedTest {
    private val colorIds = listOf(
        Color.rgb(212, 37, 50), Color.rgb(28, 174, 74), Color.rgb(38, 67, 219),
        Color.rgb(231, 182, 34), Color.rgb(168, 48, 180), Color.rgb(26, 166, 192),
    )
    private val captions = listOf(
        null to null, "한글 text" to null, null to "2026.09.22",
        "한글 text" to "2026.09.22",
    )

    @Test fun allLayoutsFiltersAndCaptionConditionsProjectTheSameScene() {
        val photos = colorIds.map { color ->
            Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        }
        try {
            var combinations = 0
            for (style in FrameLayouts.all) {
                val type = FrameType.entries.first { it.selectCount == style.slots }
                val theme = FrameCatalog.themes(type).first()
                for (filter in FilterId.entries) {
                    for ((text, date) in captions) {
                        val caption = listOfNotNull(text, date).joinToString(" · ").ifBlank { null }
                        val small = CollageLayoutMath.computeForPreview(style, theme, caption, 260f)
                        val large = CollageLayoutMath.computeForPreview(style, theme, caption, 520f)
                        val input = CollageRenderer.Input(
                            images = photos.take(style.slots), frameStyle = style, theme = theme,
                            filterId = filter, text = text, dateString = date,
                        )
                        val preview = bitmap(small.canvasWidth, small.canvasHeight)
                        val projected = bitmap(small.canvasWidth, small.canvasHeight)
                        try {
                            CollageRenderer.drawScene(Canvas(preview), input, small)
                            Canvas(projected).apply {
                                scale(small.canvasWidth / large.canvasWidth,
                                    small.canvasHeight / large.canvasHeight)
                                CollageRenderer.drawScene(this, input, large)
                            }
                            assertProjected("${style.id}/$filter/$text/$date", preview, projected)
                            combinations++
                        } finally {
                            preview.recycle()
                            projected.recycle()
                        }
                    }
                }
            }
            assertEquals(8 * 4 * 4, combinations)
        } finally {
            photos.forEach(Bitmap::recycle)
        }
    }

    @Test fun everyFrameColorAndStickerPaintsItsSelectedAsset() {
        assertEquals(45, FrameColors.all.size)
        assertEquals(25, StickerPalette.entries.size)
        val style = FrameLayouts.byId(FrameLayoutId.TWO_HORIZONTAL)
        val theme = FrameCatalog.themes(FrameType.TWO_CUT).first()
        val layout = CollageLayoutMath.computeForPreview(style, theme, null, 260f)
        for (color in FrameColors.all) {
            val restored = FrameColors.byId(color.id)
            assertEquals(color.id, restored.id)
            val scene = draw(CollageRenderer.Input(
                images = emptyList(), frameStyle = style, theme = theme,
                overrideBackground = restored.color,
                backgroundGradient = restored.gradientStops,
            ), layout.canvasWidth, layout.canvasHeight) { canvas, input ->
                CollageRenderer.drawScene(canvas, input, layout)
            }
            try {
                val nearStart = scene.getPixel(8, 8)
                if (restored.gradientStops == null) {
                    assertEquals("${color.id} flat background", restored.color.toArgb(), nearStart)
                } else {
                    val nearEnd = scene.getPixel(scene.width - 9, 8)
                    assertTrue("${color.id} gradient must vary", nearStart != nearEnd)
                }
            } finally {
                scene.recycle()
            }
        }

        val baseline = bitmap(layout.canvasWidth, layout.canvasHeight)
        CollageRenderer.drawScene(Canvas(baseline), CollageRenderer.Input(
            images = emptyList(), frameStyle = style, theme = theme,
            overrideBackground = androidx.compose.ui.graphics.Color.White,
        ), layout)
        try {
            for (sticker in StickerPalette.entries) {
                assertEquals(sticker, StickerPalette.fromAssetId(sticker.assetId))
                val rendered = bitmap(layout.canvasWidth, layout.canvasHeight)
                try {
                    CollageRenderer.drawScene(Canvas(rendered), CollageRenderer.Input(
                        images = emptyList(), frameStyle = style, theme = theme,
                        overrideBackground = androidx.compose.ui.graphics.Color.White,
                        customDecorations = listOf(CustomFrameDecoration(
                            position = NormPoint(0.5f, 0.5f),
                            kind = CustomFrameDecoration.Kind.Sticker(sticker.assetId, 0xD00000L),
                        )),
                    ), layout)
                    assertTrue("${sticker.assetId} must paint visible vector pixels",
                        changedPixels(baseline, rendered) > 25)
                } finally {
                    rendered.recycle()
                }
            }
        } finally {
            baseline.recycle()
        }
    }

    @Test fun customTextWrapsWithinTwoLineLogicalWidth() {
        val style = FrameLayouts.byId(FrameLayoutId.TWO_HORIZONTAL)
        val theme = FrameCatalog.themes(FrameType.TWO_CUT).first()
        val layout = CollageLayoutMath.computeForPreview(style, theme, null, 390f)
        val baseInput = CollageRenderer.Input(
            images = emptyList(), frameStyle = style, theme = theme,
            overrideBackground = androidx.compose.ui.graphics.Color.White,
        )
        val original = "한글 English emoji 🌸 가나다라마바사 아주 긴 프레임 글자입니다"
        val decoration = CustomFrameDecoration(
            position = NormPoint(0.5f, 0.5f),
            kind = CustomFrameDecoration.Kind.Text(original, 0xFFB00000L, 0.055f),
        )
        val blank = bitmap(layout.canvasWidth, layout.canvasHeight)
        val withText = bitmap(layout.canvasWidth, layout.canvasHeight)
        try {
            CollageRenderer.drawScene(Canvas(blank), baseInput, layout)
            CollageRenderer.drawScene(Canvas(withText), baseInput.copy(customDecorations = listOf(decoration)), layout)
            val allowedHalfWidth = (minOf(layout.canvasWidth, layout.canvasHeight) * 0.27f / 2f) + 2f
            var changed = 0
            for (y in 0 until blank.height) for (x in 0 until blank.width) {
                if (blank.getPixel(x, y) != withText.getPixel(x, y)) {
                    changed++
                    assertTrue("Text escaped its two-line width at x=$x",
                        abs(x - layout.canvasWidth / 2f) <= allowedHalfWidth)
                }
            }
            assertTrue("Text should be visible", changed > 25)
            assertEquals("Stored input must remain intact", original,
                (decoration.kind as CustomFrameDecoration.Kind.Text).content)
        } finally {
            blank.recycle()
            withText.recycle()
        }
    }

    /** Explicit opt-in because a 16MP allocation can terminate a low-memory emulator. */
    @Test fun measuredMaxOutputRetainsOneSourceBitmapAtATime() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("renderMemoryStress") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("Memory probe must use the isolated QA package", "com.pocket4cut.qa", context.packageName)
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val selection = FrameLayouts.all.map { style ->
            val theme = FrameCatalog.themes(FrameType.entries.first { it.selectCount == style.slots }).first()
            val size = if (style.id == FrameLayoutId.FOUR_VERTICAL) 1650 to 4920 else
                CollageOutputSize.forScene(CollageLayoutMath.compute(style, theme, "stress", null, 390f))
            Triple(style, theme, size)
        }.maxBy { it.third.first.toLong() * it.third.second }
        val resultBytes = selection.third.first.toLong() * selection.third.second * 4
        var maxOwnerBytes = resultBytes
        var maxPssKb = 0
        val result = CollageRenderer.render(CollageRenderer.Input(
            images = emptyList(), frameStyle = selection.first, theme = selection.second,
            text = "stress", recycleProvidedImages = true,
            imageProvider = {
                val photo = Bitmap.createBitmap(3000, 2000, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(colorIds[it % colorIds.size])
                }
                maxOwnerBytes = maxOf(maxOwnerBytes, resultBytes + photo.allocationByteCount)
                val memory = Debug.MemoryInfo()
                Debug.getMemoryInfo(memory)
                maxPssKb = maxOf(maxPssKb, memory.totalPss)
                photo
            },
        ))
        try {
            assertEquals(selection.third.first, result.width)
            assertEquals(selection.third.second, result.height)
            assertTrue("Output + one 6MP photo must fit 128MiB ownership budget",
                maxOwnerBytes <= 128L * 1024 * 1024)
            println("RENDER_MEMORY: memoryClass=${activityManager.memoryClass}MiB " +
                "layout=${selection.first.id} output=${result.width}x${result.height} " +
                "ownedPeak=$maxOwnerBytes bytes processPssSample=${maxPssKb}KiB")
        } finally {
            result.recycle()
        }
    }

    private fun bitmap(width: Float, height: Float): Bitmap = Bitmap.createBitmap(
        width.roundToInt(), height.roundToInt(), Bitmap.Config.ARGB_8888,
    )

    private inline fun draw(
        input: CollageRenderer.Input, width: Float, height: Float,
        action: (Canvas, CollageRenderer.Input) -> Unit,
    ): Bitmap = bitmap(width, height).also { action(Canvas(it), input) }

    private fun changedPixels(a: Bitmap, b: Bitmap): Int {
        var count = 0
        for (y in 0 until a.height) for (x in 0 until a.width) {
            if (a.getPixel(x, y) != b.getPixel(x, y)) count++
        }
        return count
    }

    private fun assertProjected(label: String, preview: Bitmap, projected: Bitmap) {
        assertEquals(preview.width, projected.width)
        assertEquals(preview.height, projected.height)
        var colorDelta = 0L
        var largeDifferences = 0
        var samples = 0
        for (y in 0 until preview.height step 2) for (x in 0 until preview.width step 2) {
            val a = preview.getPixel(x, y)
            val b = projected.getPixel(x, y)
            val delta = abs(Color.red(a) - Color.red(b)) +
                abs(Color.green(a) - Color.green(b)) + abs(Color.blue(a) - Color.blue(b))
            colorDelta += delta
            if (delta > 120) largeDifferences++
            samples++
        }
        assertTrue("$label mean RGB delta=${colorDelta.toDouble() / (samples * 3)}",
            colorDelta.toDouble() / (samples * 3) < 3.0)
        assertTrue("$label large delta fraction=${largeDifferences.toDouble() / samples}",
            largeDifferences.toDouble() / samples < 0.03)
    }
}
