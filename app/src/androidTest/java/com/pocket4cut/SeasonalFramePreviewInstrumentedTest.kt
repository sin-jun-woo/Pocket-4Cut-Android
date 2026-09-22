package com.pocket4cut

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.frame.CollageLayoutDimensions
import com.pocket4cut.frame.CollageLayoutMath
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.SeasonBackgroundFrameFactory
import com.pocket4cut.frame.SeasonHTMLFrameStyle
import com.pocket4cut.frame.rendering.SeasonalStickerArt
import com.pocket4cut.presentation.frameFlow.SeasonBackgroundFramePickScreen
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.theme.Season
import com.pocket4cut.ui.theme.Pocket4CutTheme
import java.io.File
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose picker tests. No app navigation, user sessions, camera, or MediaStore writes. */
@RunWith(AndroidJUnit4::class)
class SeasonalFramePreviewInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val loading = "계절 프레임을 불러오는 중입니다."
    private val loadError = "계절 프레임을 불러오지 못했습니다."

    @Test fun pickerLoadsAndSwitchesAllFourAtlasesAndCapturesActualAutumnScreen() {
        assertEquals("Only the QA package may create UI evidence", "com.pocket4cut.qa", context.packageName)
        val photos = (1..4).map { index ->
            instrumentation.context.assets.open("seasonal-demo/demo_0$index.jpg").use { stream ->
                requireNotNull(BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                    inSampleSize = 2
                }))
            }
        }
        var completed: Season? = null
        compose.setContent {
            Pocket4CutTheme {
                SeasonBackgroundFramePickScreen(
                    images = photos,
                    frameType = FrameType.FOUR_CUT,
                    frameStyle = FrameLayouts.byId(FrameLayoutId.FOUR_GRID),
                    theme = FrameCatalog.themes(FrameType.FOUR_CUT).first(),
                    onBack = {}, onDismiss = {}, onCompleted = { completed = it },
                )
            }
        }
        var autumnCapture: Bitmap? = null
        try {
            for (season in Season.entries) {
                compose.onNodeWithText(season.displayName, substring = false)
                    .performScrollTo().performClick()
                awaitArtwork()
                compose.onNodeWithText("${season.displayName} 배경 선택", substring = false)
                    .assertExists().performClick()
                compose.runOnIdle { assertEquals(season, completed) }
                scrollPickerToTop()
                val captured = compose.onRoot().captureToImage().asAndroidBitmap()
                try {
                    assertHeaderMatchesLoadedAtlas(captured, season, photos)
                    if (season == Season.AUTUMN) {
                        autumnCapture = captured.copy(Bitmap.Config.ARGB_8888, false)
                    }
                } finally {
                    captured.recycle()
                }
            }
            // Publish exactly one real screen, and only after all four switches passed.
            saveAutumnScreenshot(requireNotNull(autumnCapture))
        } finally {
            autumnCapture?.recycle()
            // Source photos remain Compose-owned until the test rule disposes the UI.
            // Let reachability reclaim them instead of recycling a published preview.
        }
    }

    @Test fun pickerActuallyDrawsLegacyAndModernSixCutLayoutsWithoutLosingTheVersion() {
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN)
        val photos = colors.map { color ->
            Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        }
        var version by mutableIntStateOf(1)
        compose.setContent {
            Pocket4CutTheme {
                SeasonBackgroundFramePickScreen(
                    images = photos,
                    frameType = FrameType.SIX_CUT,
                    frameStyle = FrameLayouts.byId(FrameLayoutId.SIX_COLLAGE),
                    theme = FrameCatalog.themes(FrameType.SIX_CUT).first(),
                    layoutVersion = version,
                    onBack = {}, onDismiss = {}, onCompleted = {},
                )
            }
        }
        awaitArtwork()
        for (expectedVersion in listOf(1, 2, 1)) {
            compose.runOnIdle { version = expectedVersion }
            scrollPickerToTop()
            val captured = compose.onRoot().captureToImage().asAndroidBitmap()
            try {
                val frame = displayedFrame(captured, FrameLayoutId.SIX_COLLAGE, expectedVersion)
                // At logical (190,130), v1 owns photo #2; v2's large hero owns photo #1.
                val x = (frame.left + 190f * frame.layout.scale).roundToInt()
                val y = (frame.top + 130f * frame.layout.scale).roundToInt()
                val expected = if (expectedVersion == 1) Color.GREEN else Color.RED
                assertTrue("Actual picker retained the wrong six-cut layout version: $expectedVersion",
                    rgbDistance(captured.getPixel(x, y), expected) <= 3)
            } finally {
                captured.recycle()
            }
        }
    }

    private fun awaitArtwork() {
        compose.waitUntil(15_000L) {
            compose.onAllNodesWithText(loading, substring = false).fetchSemanticsNodes().isEmpty()
        }
        compose.waitForIdle()
        compose.onNodeWithText(loading, substring = false).assertDoesNotExist()
        compose.onNodeWithText(loadError, substring = false).assertDoesNotExist()
    }

    private fun scrollPickerToTop() {
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, -100_000f)
        }
        compose.waitForIdle()
    }

    private data class DisplayedFrame(val left: Int, val top: Int, val layout: CollageLayoutDimensions)

    private fun displayedFrame(screen: Bitmap, id: FrameLayoutId, version: Int): DisplayedFrame {
        val style = FrameLayouts.byId(id)
        val type = FrameType.entries.first { it.selectCount == style.slots }
        val theme = FrameCatalog.themes(type).first()
        val padding = with(compose.density) { AppSpacing.Screen.horizontal.roundToPx() }
        val holderHeight = with(compose.density) { 480.dp.roundToPx() }
        val availableWidth = screen.width - padding * 2
        val unscaled = CollageLayoutMath.computeForPreview(style, theme, null, availableWidth.toFloat(), version)
        val fit = min(1f, holderHeight.toFloat() / unscaled.canvasHeight.roundToInt())
        val width = (availableWidth * fit).roundToInt()
        val height = (unscaled.canvasHeight.roundToInt() * fit).roundToInt()
        val scrollTop = compose.onNode(hasScrollAction()).fetchSemanticsNode().boundsInRoot.top.roundToInt()
        return DisplayedFrame(
            padding + ((availableWidth - width) / 2f).roundToInt(),
            scrollTop + ((holderHeight - height) / 2f).roundToInt(),
            CollageLayoutMath.computeForPreview(style, theme, null, width.toFloat(), version),
        )
    }

    private fun assertHeaderMatchesLoadedAtlas(screen: Bitmap, season: Season, photos: List<Bitmap>) {
        val frame = displayedFrame(screen, FrameLayoutId.FOUR_GRID, 2)
        val sheet = runBlocking { SeasonalStickerArt.load(context, season) }
        val expected = Bitmap.createBitmap(frame.layout.canvasWidth.roundToInt(),
            frame.layout.canvasHeight.roundToInt(), Bitmap.Config.ARGB_8888)
        try {
            CollageRenderer.drawScene(Canvas(expected), CollageRenderer.Input(
                images = photos,
                frameStyle = FrameLayouts.byId(FrameLayoutId.FOUR_GRID),
                theme = FrameCatalog.themes(FrameType.FOUR_CUT).first(),
                customFrameDesign = SeasonBackgroundFrameFactory.design(season),
                seasonalArt = sheet,
            ), frame.layout)
            val paper = (0xFF000000 or SeasonHTMLFrameStyle.baseHex(season)).toInt()
            val scale = frame.layout.scale
            var samples = 0
            var totalDifference = 0L
            // Only distinctive illustration pixels in the two header corners are
            // compared: stock color and title text cannot satisfy this assertion.
            for (y in (5f * scale).roundToInt() until (frame.layout.headerArea.bottom - 5f * scale).roundToInt() step 2) {
                for (x in (5f * scale).roundToInt() until (90f * scale).roundToInt() step 2) {
                    for (sampleX in listOf(x, expected.width - 1 - x)) {
                        val wanted = expected.getPixel(sampleX, y)
                        if (rgbDistance(wanted, paper) < 45) continue
                        var difference = 255
                        // GPU layer scaling and software Canvas can disagree by a
                        // subpixel; a one-pixel registration tolerance keeps this
                        // focused on the actual artwork rather than antialiasing.
                        for (dy in -1..1) for (dx in -1..1) {
                            val sx = (frame.left + sampleX + dx).coerceIn(0, screen.width - 1)
                            val sy = (frame.top + y + dy).coerceIn(0, screen.height - 1)
                            difference = min(difference, rgbDistance(screen.getPixel(sx, sy), wanted))
                        }
                        samples++
                        totalDifference += difference
                    }
                }
            }
            assertTrue("$season must expose real illustrated header pixels", samples > 40)
            val meanDifference = totalDifference.toDouble() / samples
            assertTrue("Actual picker does not show the requested $season atlas; mean RGB error=$meanDifference",
                meanDifference < 24.0)
        } finally {
            expected.recycle()
        }
    }

    private fun rgbDistance(a: Int, b: Int): Int = maxOf(
        abs(Color.red(a) - Color.red(b)), abs(Color.green(a) - Color.green(b)), abs(Color.blue(a) - Color.blue(b)),
    )

    private fun saveAutumnScreenshot(bitmap: Bitmap) {
        val configured = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val outputRoot = configured?.takeIf { it.isNotBlank() }?.let { path ->
            val requested = File(path).canonicalFile
            @Suppress("DEPRECATION")
            val allowed = context.externalMediaDirs.filterNotNull().map {
                File(it, "additional_test_output").canonicalFile
            }
            require(requested in allowed) { "UI evidence must stay in the QA additional output directory" }
            requested
        } ?: requireNotNull(context.getExternalFilesDir(null)).canonicalFile
        val directory = File(outputRoot, "seasonal-ui").canonicalFile
        require(directory.path.startsWith(outputRoot.path + File.separator))
        assertTrue(directory.isDirectory || directory.mkdirs())
        val output = File(directory, "autumn_picker_compose_${System.currentTimeMillis()}.png")
        output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        instrumentation.sendStatus(0, Bundle().apply { putString("seasonal_ui_screenshot", output.absolutePath) })
    }
}
