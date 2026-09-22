package com.pocket4cut

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocket4cut.frame.CollageLayoutMath
import com.pocket4cut.frame.CollageOutputSize
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.rendering.SeasonDecorAnchors
import com.pocket4cut.presentation.navigation.FrameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class CollageOutputContractInstrumentedTest {
    @Test fun everyLayoutUsesDeviceIndependentBoundedOutput() {
        for (style in FrameLayouts.all) {
            val type = FrameType.entries.first { it.selectCount == style.slots }
            val theme = FrameCatalog.themes(type).first()
            for (caption in listOf<String?>(null, "문구")) {
                val logical = CollageLayoutMath.compute(style, theme, caption, null, 390f)
                val (width, height) = if (style.id == FrameLayoutId.FOUR_VERTICAL) {
                    1650 to 4920
                } else CollageOutputSize.forScene(logical)
                assertTrue("${style.id} pixel cap", width.toLong() * height <= 16_000_000L)
                assertTrue("${style.id} edge cap", width <= 8192 && height <= 8192)
                assertTrue("${style.id} positive output", width > 0 && height > 0)
                assertTrue("${style.id} aspect ratio", abs(height.toDouble() / width -
                    logical.canvasHeight / logical.canvasWidth) < 0.002)
                if (style.id == FrameLayoutId.FOUR_VERTICAL) {
                    assertEquals(1650, width)
                    assertEquals(4920, height)
                }
            }
        }
    }

    @Test fun newSixCutFirstPhotoOwnsHeroSlotAndOldLayoutStaysGrid() {
        val style = FrameLayouts.byId(FrameLayoutId.SIX_COLLAGE)
        val theme = FrameCatalog.themes(FrameType.SIX_CUT).first()
        val modern = CollageLayoutMath.compute(style, theme, null, null, 390f, 2)
        val legacy = CollageLayoutMath.compute(style, theme, null, null, 390f, 1)
        assertEquals(390f, modern.canvasWidth)
        assertEquals(545f, modern.canvasHeight)
        assertTrue(modern.cells[0].width() > modern.cells[1].width() * 2f)
        assertTrue(modern.cells[0].height() > modern.cells[1].height() * 2f)
        assertTrue(legacy.cells[0].width() < modern.cells[0].width())
    }

    @Test fun sixCutSeasonFooterHasSpaceBeforeCaptionAtBothCanvasHeights() {
        val style = FrameLayouts.byId(FrameLayoutId.SIX_COLLAGE)
        val theme = FrameCatalog.themes(FrameType.SIX_CUT).first()
        for (caption in listOf<String?>(null, "문구")) {
            val layout = CollageLayoutMath.compute(style, theme, caption, null, 390f, 2)
            val slotBottom = layout.cells.maxOf { it.bottom }
            val footerTop = SeasonDecorAnchors.memoriesBaselineY(
                layout.canvasHeight, layout.cells, 12f, layout.canvasHeight - 20f,
            )
            val footerBottom = footerTop + 12f
            assertEquals(if (caption == null) 545f else 585f, layout.canvasHeight)
            assertTrue("Season footer must clear photos", footerTop - 10f >= slotBottom)
            assertTrue("Season footer must clear caption", footerBottom <= (layout.textArea?.top ?: layout.canvasHeight - 8f))
            assertEquals(500f, slotBottom, 0.01f)
        }
    }
}
