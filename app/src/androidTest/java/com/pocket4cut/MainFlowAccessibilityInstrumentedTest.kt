package com.pocket4cut

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocket4cut.frame.CustomFrameDecoration
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.NormPoint
import com.pocket4cut.presentation.detailEdit.AdjustmentSliderRow
import com.pocket4cut.presentation.edit.DateToggleSection
import com.pocket4cut.presentation.edit.EditUiState
import com.pocket4cut.presentation.edit.OrderSection
import com.pocket4cut.presentation.frameFlow.DecorationTransformControls
import com.pocket4cut.presentation.selection.photoSelectionSemantics
import com.pocket4cut.ui.designsystem.components.PinkGradientSlider
import com.pocket4cut.ui.theme.Pocket4CutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class MainFlowAccessibilityInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectedPhotosAnnounceOrderAndKeepDeselectionAvailableAtLimit() {
        var selected by mutableStateOf(listOf(2, 0))
        compose.setContent {
            Pocket4CutTheme {
                Column {
                    repeat(3) { index ->
                        val order = selected.indexOf(index).takeIf { it >= 0 }?.plus(1)
                        Box(Modifier.size(80.dp).photoSelectionSemantics(index + 1, order, selected.size < 2) {
                            selected = if (index in selected) selected - index else selected + index
                        })
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("촬영 사진 1")
            .assertIsOn().assertIsEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "선택됨, 배치 순서 2번째"))
        compose.onNodeWithContentDescription("촬영 사진 2").assertIsOff().assertIsNotEnabled()
        compose.onNodeWithContentDescription("촬영 사진 3").performClick()
        compose.onNodeWithContentDescription("촬영 사진 1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "선택됨, 배치 순서 1번째"))
        compose.onNodeWithContentDescription("촬영 사진 2").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(listOf(0, 1), selected) }
    }

    @Test fun dateSwitchAnnouncesStateAndRevealsAdjustableDateSize() {
        var showDate by mutableStateOf(false)
        var dateSize by mutableStateOf(16f)
        compose.setContent {
            Pocket4CutTheme {
                DateToggleSection(
                    EditUiState(showDate = showDate, dateString = "2026.09.22", dateFontSize = dateSize),
                    onShowDateChange = { showDate = it },
                    onDateFontSizeChange = { dateSize = it },
                )
            }
        }
        compose.onNodeWithContentDescription("날짜 표시")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            .assertIsOff().performClick().assertIsOn()
        compose.onNodeWithContentDescription("날짜 크기")
            .performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(24f)) }
        compose.runOnIdle { assertEquals(24f, dateSize) }
    }

    @Test fun customSlidersSupportScreenReaderRangeAndKeyboardWithoutDragging() {
        var fontSize by mutableStateOf(16f)
        var brightness by mutableStateOf(0f)
        compose.setContent {
            Pocket4CutTheme {
                Column {
                    PinkGradientSlider(fontSize, { fontSize = it }, 1f..30f, "글자 크기")
                    AdjustmentSliderRow("밝기", brightness) { brightness = it }
                }
            }
        }
        compose.onNodeWithContentDescription("글자 크기")
            .assertHeightIsAtLeast(48.dp)
            .performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(100f)) }
        compose.runOnIdle { assertEquals(30f, fontSize) }
        compose.onNodeWithContentDescription("밝기")
            .assertHeightIsAtLeast(48.dp)
            .performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(-75f)) }
        compose.runOnIdle { assertEquals(-50f, brightness) }
        compose.onNodeWithContentDescription("밝기")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithContentDescription("밝기").performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { assertEquals(-45f, brightness) }
        compose.onNodeWithContentDescription("밝기").performKeyInput { pressKey(Key.MoveEnd) }
        compose.runOnIdle { assertEquals(50f, brightness) }
    }

    @Test fun photoOrderOffersExplicitMoveActionsAndWholePhotoSwapTarget() {
        val images = List(2) { Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888) }
        var move: Pair<Int, Int>? = null
        var selected: Int? = null
        compose.setContent {
            Pocket4CutTheme {
                OrderSection(
                    EditUiState(orderedImages = images, showDate = false),
                    FrameLayouts.defaultForSlots(2),
                    onTapCell = { selected = it },
                    onMoveSlot = { from, to -> move = from to to },
                )
            }
        }
        val first = compose.onNodeWithContentDescription("1번째 사진 순서 바꾸기")
        first.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        val actions = first.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle {
            assertEquals(0, selected)
            assertEquals(listOf("뒤로 한 칸 이동"), actions.map { it.label })
            assertTrue(actions.single().action())
            assertEquals(0 to 1, move)
        }
    }

    @Test fun decorationCanMoveScaleAndRotateWithSingleTapControls() {
        var decoration by mutableStateOf(CustomFrameDecoration(kind = CustomFrameDecoration.Kind.Emoji("★")))
        compose.setContent {
            Pocket4CutTheme { DecorationTransformControls(decoration, { decoration = it }) }
        }
        compose.onNodeWithContentDescription("장식 오른쪽으로 이동")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        compose.onNodeWithContentDescription("장식 아래로 이동").performClick()
        compose.onNodeWithContentDescription("장식 크게").performClick()
        compose.onNodeWithContentDescription("장식 오른쪽으로 15도 회전").performClick()
        compose.runOnIdle {
            assertEquals(0.52f, decoration.position.x, 0.0001f)
            assertEquals(0.52f, decoration.position.y, 0.0001f)
            assertEquals(1.1f, decoration.scale, 0.0001f)
            assertEquals(15.0, Math.toDegrees(decoration.rotationRadians.toDouble()), 0.001)
            decoration = decoration.copy(position = NormPoint(0.02f, 0.98f), scale = 0.25f)
        }
        compose.onNodeWithContentDescription("장식 왼쪽으로 이동").assertIsNotEnabled()
        compose.onNodeWithContentDescription("장식 아래로 이동").assertIsNotEnabled()
        compose.onNodeWithContentDescription("장식 작게").assertIsNotEnabled()
        compose.onNodeWithContentDescription("장식 크게").assertIsEnabled()
    }
}
