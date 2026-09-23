package com.pocket4cut

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocket4cut.domain.model.InputSource
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.presentation.capture.GlassmorphismCircle
import com.pocket4cut.presentation.capture.ImmediateShutterButton
import com.pocket4cut.presentation.edit.FilterChip
import com.pocket4cut.presentation.frameFlow.ColorChip
import com.pocket4cut.presentation.frameFlow.FrameModeRow
import com.pocket4cut.presentation.frameFlow.SeasonCard
import com.pocket4cut.presentation.frameTypeSelect.FrameTypeRow
import com.pocket4cut.presentation.gallery.GalleryViewMode
import com.pocket4cut.presentation.gallery.GalleryViewModeToggle
import com.pocket4cut.presentation.gallery.GallerySection
import com.pocket4cut.presentation.gallery.GallerySectionTabs
import com.pocket4cut.presentation.layoutSelection.LayoutCard
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.presentation.settings.ToggleRow
import com.pocket4cut.ui.designsystem.components.AppToast
import com.pocket4cut.ui.designsystem.components.AppToastType
import com.pocket4cut.ui.designsystem.components.ColorPaletteCell
import com.pocket4cut.ui.designsystem.theme.Season
import com.pocket4cut.ui.theme.Pocket4CutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReleaseAccessibilityInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captureControlsExposeNamedButtonStateAndMinimumTouchTargets() {
        var zoomEnabled by mutableStateOf(true)
        var zoomClicks = 0
        var manualClicks = 0
        compose.setContent {
            Pocket4CutTheme {
                Column {
                    GlassmorphismCircle(
                        onClick = { zoomClicks += 1 },
                        diameter = 44.dp,
                        accessibilityLabel = "확대",
                        accessibilityState = "현재 1.0배",
                        enabled = zoomEnabled,
                    ) { Box(Modifier.size(20.dp)) }
                    ImmediateShutterButton(onClick = { manualClicks += 1 })
                }
            }
        }

        val zoom = compose.onNodeWithContentDescription("확대")
        zoom.assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "현재 1.0배"))
            .assertIsEnabled()
            .performClick()
        compose.onNodeWithContentDescription("카운트다운 건너뛰고 바로 촬영")
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        compose.runOnIdle {
            assertEquals(1, zoomClicks)
            assertEquals(1, manualClicks)
            zoomEnabled = false
        }
        zoom.assertIsNotEnabled()
    }

    @Test fun settingsToastAndPaletteExposeSinglePurposeSemantics() {
        var checked by mutableStateOf(false)
        var dismissed = 0
        var selectedColor by mutableStateOf<Long?>(null)
        compose.setContent {
            Pocket4CutTheme {
                Column {
                    ToggleRow(
                        title = "전면 카메라 기본",
                        subtitle = "촬영 시작 시 셀카 모드로",
                        icon = Icons.Default.CameraAlt,
                        checked = checked,
                        onCheckedChange = { checked = it },
                    )
                    AppToast("저장 완료", AppToastType.Success, onDismiss = { dismissed += 1 })
                    Column(Modifier.selectableGroup()) {
                        ColorPaletteCell(
                            rgb = null,
                            isAuto = true,
                            accessibilityLabel = "자동 색상",
                            selected = selectedColor == null,
                            onClick = { selectedColor = null },
                        )
                        ColorPaletteCell(
                            rgb = 0x000000L,
                            isAuto = false,
                            accessibilityLabel = "검정 색상",
                            selected = selectedColor == 0x000000L,
                            onClick = { selectedColor = 0x000000L },
                        )
                    }
                }
            }
        }

        compose.onNodeWithContentDescription("전면 카메라 기본")
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOff()
            .performClick()
            .assertIsOn()
        compose.onNodeWithText("저장 완료")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNodeWithContentDescription("알림 닫기")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        compose.onNodeWithContentDescription("자동 색상")
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        compose.onNodeWithContentDescription("검정 색상")
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
            .performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        compose.runOnIdle { assertEquals(1, dismissed) }
    }

    @Test fun frameLayoutSeasonAndColorChoicesExposeRadioSelection() {
        val frameStyle = FrameLayouts.defaultForSlots(2)
        val frameColor = FrameColors.all.first()
        compose.setContent {
            Pocket4CutTheme {
                Column {
                    FrameTypeRow(
                        index = 1,
                        type = FrameType.TWO_CUT,
                        layoutCount = FrameLayouts.bySlots(2).size,
                        selected = true,
                        inputSource = InputSource.CAMERA,
                        onClick = {},
                    )
                    LayoutCard(frameStyle, isSelected = true, onSelect = {})
                    SeasonCard(Season.SPRING, "벚꽃 · 튤립 · 리본", selected = true, onClick = {})
                    ColorChip(frameColor, selected = true, onClick = {})
                }
            }
        }

        val labels = listOf(
            "${FrameType.TWO_CUT.selectCount}컷 구성, ${FrameType.TWO_CUT.subtitle}, 레이아웃 ${FrameLayouts.bySlots(2).size}종",
            "레이아웃 ${frameStyle.name}",
            "계절 배경 ${Season.SPRING.displayName}",
            "프레임 색상 ${frameColor.name}",
        )
        labels.forEach { label ->
            compose.onNodeWithContentDescription(label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        }
    }

    @Test fun frameModesGalleryTabsAndEditFiltersExposeCorrectRoles() {
        var galleryMode by mutableStateOf(GalleryViewMode.BY_DATE)
        var gallerySection by mutableStateOf(GallerySection.COMPLETED)
        var filter by mutableStateOf(FilterId.ORIGINAL)
        compose.setContent {
            Pocket4CutTheme {
                Column {
                    FrameModeRow(
                        code = "A",
                        icon = Icons.Default.Palette,
                        title = "COLOR",
                        subtitle = "한 가지 색으로 또렷하게",
                        onClick = {},
                    )
                    GallerySectionTabs(
                        selectedSection = gallerySection,
                        completedCount = 2,
                        draftCount = 1,
                        onSectionSelected = { gallerySection = it },
                    )
                    GalleryViewModeToggle(
                        selectedMode = galleryMode,
                        onModeSelected = { galleryMode = it },
                    )
                    Row(Modifier.selectableGroup()) {
                        FilterChip(
                            filter = FilterId.ORIGINAL,
                            isSelected = filter == FilterId.ORIGINAL,
                            thumbnail = null,
                            onClick = { filter = FilterId.ORIGINAL },
                        )
                        FilterChip(
                            filter = FilterId.SOFT,
                            isSelected = filter == FilterId.SOFT,
                            thumbnail = null,
                            onClick = { filter = FilterId.SOFT },
                        )
                    }
                }
            }
        }

        compose.onNodeWithContentDescription("프레임 방식 COLOR, 한 가지 색으로 또렷하게")
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        compose.onNodeWithContentDescription("완성 작업, 2개")
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        compose.onNodeWithContentDescription("진행 중인 작업, 1개")
            .performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        compose.onNodeWithContentDescription("전체 보기")
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        compose.onNodeWithContentDescription("종류별 보기")
            .performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        compose.onAllNodesWithContentDescription("필터 원본").assertCountEquals(1)
        compose.onNodeWithContentDescription("필터 원본")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        compose.onNodeWithContentDescription("필터 소프트")
            .performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }
}
