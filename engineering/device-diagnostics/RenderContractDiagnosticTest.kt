package com.pocket4cut.diagnostic

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.frame.CollageLayoutMath
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.frame.CustomFrameDecoration
import com.pocket4cut.frame.FilterDefs
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.NormPoint
import com.pocket4cut.frame.StickerPalette
import com.pocket4cut.presentation.detailEdit.DetailEditViewModel
import com.pocket4cut.presentation.navigation.FrameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Device diagnostics against production rendering/state code. Fixtures are synthetic numbered
 * bitmaps only. Never calls storage.saveResult, SessionRepository, PendingCollageStore or MediaStore;
 * never reads, modifies or deletes existing user photos or sessions. Known contract failures are
 * deliberately asserted as failures, not inverted into passing tests.
 */
@RunWith(AndroidJUnit4::class)
class RenderContractDiagnosticTest {
    private val fixtureColors = listOf(
        Color.rgb(210, 30, 45), Color.rgb(25, 175, 70), Color.rgb(35, 70, 220),
        Color.rgb(235, 180, 25), Color.rgb(180, 45, 185), Color.rgb(25, 175, 190),
    )

    @Test
    fun allEightLayoutsAndFourFiltersRenderEveryNumberedPhotoInInputOrder() {
        val images = numberedFixtures()
        val failures = mutableListOf<String>()
        try {
            for (style in FrameLayouts.all) {
                val theme = FrameCatalog.themes(frameType(style)).first()
                for (filter in FilterId.entries) {
                    val result = CollageRenderer.render(
                        images.take(style.slots), style, theme, null, filter,
                        null, null, 390,
                    )
                    try {
                        val geometry = CollageLayoutMath.compute(style, theme, null, null, 390f)
                        assertEquals("${style.id}: width", geometry.canvasWidth.toInt(), result.width)
                        assertTrue("${style.id}: height", abs(geometry.canvasHeight - result.height) <= 1f)
                        geometry.cells.forEachIndexed { index, cell ->
                            // The number is in the upper-left corner; the center is uniform color.
                            val actual = result.getPixel(cell.centerX().toInt(), cell.centerY().toInt())
                            val expected = filteredColor(fixtureColors[index], filter)
                            if (!sameRgb(actual, expected)) {
                                failures += "${style.id}/$filter slot ${index + 1}: " +
                                    "expected ${hex(expected)}, actual ${hex(actual)}"
                            }
                        }
                    } finally {
                        result.recycle()
                    }
                }
            }
        } finally {
            images.forEach(Bitmap::recycle)
        }
        assertTrue("32 render combinations: ${failures.joinToString("; ")}", failures.isEmpty())
    }

    @Test
    fun rendererHonorsDeliberatelyReversedPhotoInputOrder() {
        val images = numberedFixtures()
        val style = FrameLayouts.byId(FrameLayoutId.FOUR_GRID)
        val theme = FrameCatalog.themes(FrameType.FOUR_CUT).first()
        val order = listOf(3, 2, 1, 0)
        try {
            val result = CollageRenderer.render(
                order.map { images[it] }, style, theme, null, FilterId.ORIGINAL,
                null, null, 390,
            )
            try {
                val geometry = CollageLayoutMath.compute(style, theme, null, null, 390f)
                geometry.cells.forEachIndexed { index, cell ->
                    assertEquals(
                        "Slot ${index + 1} must use source photo ${order[index] + 1}",
                        fixtureColors[order[index]],
                        result.getPixel(cell.centerX().toInt(), cell.centerY().toInt()),
                    )
                }
            } finally {
                result.recycle()
            }
        } finally {
            images.forEach(Bitmap::recycle)
        }
    }

    @Test
    fun captionAndDateReserveTheSameAreaInPreviewAndExportForAllLayouts() {
        val failures = mutableListOf<String>()
        for (style in FrameLayouts.all) {
            val theme = FrameCatalog.themes(frameType(style)).first()
            for ((text, date) in listOf("AUDIT" to null, null to "2026.09.10", "AUDIT" to "2026.09.10")) {
                val caption = listOfNotNull(text, date).joinToString(" · ")
                val preview = CollageLayoutMath.computeForPreview(style, theme, caption, 390f)
                val export = CollageLayoutMath.compute(style, theme, text, date, 390f)
                val scale = preview.canvasWidth / export.canvasWidth
                if (preview.textArea == null || export.textArea == null) {
                    failures += "${style.id}/$caption: preview=${preview.textArea}, export=${export.textArea}"
                } else if (abs(preview.textArea!!.height() - export.textArea!!.height() * scale) > 0.01f) {
                    failures += "${style.id}/$caption: caption band differs"
                }
                if (abs(preview.canvasHeight - export.canvasHeight * scale) > 0.01f) {
                    failures += "${style.id}/$caption: total height differs"
                }
                preview.cells.zip(export.cells).forEachIndexed { index, (p, e) ->
                    if (abs(p.top - e.top * scale) > 0.01f || abs(p.bottom - e.bottom * scale) > 0.01f) {
                        failures += "${style.id}/$caption: slot ${index + 1} crop geometry differs"
                    }
                }
            }
        }
        assertTrue("Caption geometry contracts: ${failures.joinToString("; ")}", failures.isEmpty())
    }

    @Test
    fun everySelectableFramePaletteIdRestoresTheChosenRgb() {
        val failures = mutableListOf<String>()
        for (type in FrameType.entries) {
            for (chosen in FrameColors.colorFramePalette(type)) {
                val restored = FrameColors.byId(chosen.id)
                if (chosen.color.toArgb() != restored.color.toArgb()) {
                    failures += "${type.id}/${chosen.id}: ${hex(chosen.color.toArgb())} -> " +
                        "${restored.id}/${hex(restored.color.toArgb())}"
                }
            }
        }
        assertTrue("Selectable color IDs must round-trip: ${failures.joinToString("; ")}", failures.isEmpty())
    }

    @Test
    fun freelyArrangedSixCollageIsGeometricallyDifferentFromSixHorizontalGrid() {
        val theme = FrameCatalog.themes(FrameType.SIX_CUT).first()
        val grid = CollageLayoutMath.compute(
            FrameLayouts.byId(FrameLayoutId.SIX_GRID_3X2), theme, null, null, 390f,
        )
        val collage = CollageLayoutMath.compute(
            FrameLayouts.byId(FrameLayoutId.SIX_COLLAGE), theme, null, null, 390f,
        )
        val identical = grid.canvasWidth == collage.canvasWidth &&
            grid.canvasHeight == collage.canvasHeight && grid.cells == collage.cells
        assertFalse("'자유로운 배치' must not render exactly the same cells as the 3x2 grid", identical)
    }

    @Test
    fun exportedHeartStickerIsNotIdenticalToPlainTextOfItsDisplayName() {
        val style = FrameLayouts.byId(FrameLayoutId.FOUR_HORIZONTAL)
        val theme = FrameCatalog.themes(FrameType.FOUR_CUT).first()
        val sticker = CustomFrameDecoration(
            position = NormPoint(0.5f, 0.5f),
            kind = CustomFrameDecoration.Kind.Sticker(StickerPalette.HEART.assetId, 0xD00000L),
        )
        val label = sticker.copy(
            kind = CustomFrameDecoration.Kind.Text(StickerPalette.HEART.displayName, 0xFFD00000L, 0.12f),
        )
        val input = CollageRenderer.Input(
            images = emptyList(), frameStyle = style, theme = theme,
            customDecorations = listOf(sticker), context = null,
        )
        val graphicResult = CollageRenderer.render(input)
        try {
            val textResult = CollageRenderer.render(input.copy(customDecorations = listOf(label)))
            try {
                assertFalse(
                    "The selected heart graphic exports pixel-identically to its Korean label '하트'",
                    graphicResult.sameAs(textResult),
                )
            } finally {
                textResult.recycle()
            }
        } finally {
            graphicResult.recycle()
        }
    }

    @Test
    fun replacingDetailPreviewDoesNotRecycleBitmapStillRetainedByUiConsumer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as Application
        val store = ViewModelStore()
        val baseImages = numberedFixtures(count = 2)
        lateinit var viewModel: DetailEditViewModel
        try {
            instrumentation.runOnMainSync {
                viewModel = ViewModelProvider(
                    store, ViewModelProvider.AndroidViewModelFactory(app),
                )[DetailEditViewModel::class.java]
                viewModel.initialize(
                    baseImages = baseImages, imagePaths = emptyList(),
                    frameType = FrameType.TWO_CUT,
                    frameStyle = FrameLayouts.byId(FrameLayoutId.TWO_HORIZONTAL),
                    theme = FrameCatalog.themes(FrameType.TWO_CUT).first(),
                    frameColor = FrameColors.byId("white"), globalFilter = FilterId.ORIGINAL,
                    customText = "", showDate = false,
                    sessionId = "diagnostic-in-memory-only", selectedIndexes = listOf(0, 1),
                )
            }
            awaitCondition("initial preview") { viewModel.uiState.value.collagePreviewImages.size == 2 }
            // Models a consumer holding the previous StateFlow emission until its next draw.
            // It is a lifetime contract test, not a claim of deterministic Compose crash timing.
            val previouslyPublished = viewModel.uiState.value.collagePreviewImages[0]
            instrumentation.runOnMainSync { viewModel.setBrightness(0.1f) }
            awaitCondition("replacement preview") {
                viewModel.uiState.value.collagePreviewImages.firstOrNull() !== previouslyPublished
            }
            assertFalse(
                "The previously published bitmap was recycled before a retained UI consumer released it",
                previouslyPublished.isRecycled,
            )
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            baseImages.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    private fun frameType(style: FrameStyle): FrameType =
        FrameType.entries.first { it.selectCount == style.slots }

    private fun numberedFixtures(count: Int = 6): List<Bitmap> = fixtureColors.take(count).mapIndexed { index, color ->
        Bitmap.createBitmap(180, 240, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
            Canvas(this).drawText("${index + 1}", 8f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                textSize = 24f
            })
        }
    }

    private fun filteredColor(color: Int, filter: FilterId): Int {
        val source = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        val target = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        return try {
            Canvas(target).drawBitmap(source, 0f, 0f, Paint().apply { colorFilter = FilterDefs.colorFilter(filter) })
            target.getPixel(0, 0)
        } finally {
            source.recycle()
            target.recycle()
        }
    }

    private fun sameRgb(a: Int, b: Int): Boolean = abs(Color.red(a) - Color.red(b)) <= 2 &&
        abs(Color.green(a) - Color.green(b)) <= 2 && abs(Color.blue(a) - Color.blue(b)) <= 2

    private fun hex(color: Int): String = Integer.toHexString(color)

    private fun awaitCondition(label: String, predicate: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (!predicate() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(10)
        assertTrue("Timed out waiting for $label", predicate())
    }
}
