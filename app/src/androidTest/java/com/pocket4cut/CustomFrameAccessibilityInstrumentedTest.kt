package com.pocket4cut

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocket4cut.frame.CustomFrameDecoration
import com.pocket4cut.frame.CustomFrameDesign
import com.pocket4cut.frame.EmojiPicklist
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.StickerPalette
import com.pocket4cut.presentation.frameFlow.CustomFrameEditorScreen
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.theme.Pocket4CutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual editor controls in a portrait viewport; no camera, settings or session files are used. */
@RunWith(AndroidJUnit4::class)
class CustomFrameAccessibilityInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emojiTrayCanAddAndContinueInPortrait() =
        assertTrayCanAddAndContinue(FrameType.TWO_CUT, fontScale = 1f, sticker = false)

    @Test fun emojiTrayCanAddAndContinueInPortraitWithDoubleFont() =
        assertTrayCanAddAndContinue(FrameType.SIX_CUT, fontScale = 2f, sticker = false)

    @Test fun stickerTrayCanAddAndContinueInPortrait() =
        assertTrayCanAddAndContinue(FrameType.FOUR_CUT, fontScale = 1f, sticker = true)

    @Test fun stickerTrayCanAddAndContinueInPortraitWithDoubleFont() =
        assertTrayCanAddAndContinue(FrameType.SIX_CUT, fontScale = 2f, sticker = true)

    private fun assertTrayCanAddAndContinue(frameType: FrameType, fontScale: Float, sticker: Boolean) {
        val photos = List(frameType.selectCount) { index ->
            Bitmap.createBitmap(16, 24, Bitmap.Config.ARGB_8888).apply {
                eraseColor(if (index % 2 == 0) Color.RED else Color.BLUE)
            }
        }
        val completed = mutableListOf<CustomFrameDesign>()
        val emoji = EmojiPicklist.all.first()
        val selectedSticker = StickerPalette.HEART
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Box(Modifier.size(width = 360.dp, height = 640.dp)) {
                    Pocket4CutTheme {
                        CustomFrameEditorScreen(
                            images = photos,
                            frameType = frameType,
                            frameStyle = FrameLayouts.defaultForSlots(frameType.selectCount),
                            theme = FrameCatalog.themes(frameType).first(),
                            onBack = { _, _ -> },
                            onDismiss = { _, _ -> },
                            onCompleted = { design, _ -> completed += design },
                        )
                    }
                }
            }
        }

        compose.onNodeWithText(if (sticker) "스티커" else "이모지")
            .performScrollTo().assertIsDisplayed().performClick()
        if (sticker) {
            compose.onNodeWithContentDescription(selectedSticker.displayName)
                .performScrollTo().assertIsDisplayed().performClick()
        } else {
            compose.onNodeWithText(emoji)
                .performScrollTo().assertIsDisplayed().performClick()
        }
        compose.runOnIdle { assertTrue("Adding a decoration must not navigate", completed.isEmpty()) }

        // The tray stays open and the selected decoration adds more controls above this CTA.
        // Scrolling must reach the real button, not invoke its callback directly.
        compose.onNodeWithText("이 프레임으로 계속")
            .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, completed.size)
            val design = completed.single()
            assertEquals(FrameColors.all.first().id, design.fillColorId)
            assertEquals(1, design.decorations.size)
            val decoration = design.decorations.single()
            if (sticker) {
                assertTrue(decoration.kind is CustomFrameDecoration.Kind.Sticker)
                assertEquals(selectedSticker.assetId, (decoration.kind as CustomFrameDecoration.Kind.Sticker).assetId)
            } else {
                assertEquals(CustomFrameDecoration.Kind.Emoji(emoji), decoration.kind)
            }
        }
    }
}
