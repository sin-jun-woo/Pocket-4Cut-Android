package com.pocket4cut

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.presentation.photoImport.ImportedPhotoItem
import com.pocket4cut.presentation.photoImport.PhotoImportContent
import com.pocket4cut.presentation.photoImport.PhotoImportUiState
import com.pocket4cut.ui.theme.Pocket4CutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoImportAccessibilityInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun exactCountControlsContinuation() {
        compose.setContent {
            Pocket4CutTheme {
                PhotoImportContent(
                    frameType = FrameType.FOUR_CUT,
                    uiState = PhotoImportUiState(isInitialized = true),
                    onBack = {}, onOpenAlbum = {}, onMove = { _, _ -> }, onRemove = {},
                    onDismissMessage = {}, onDone = {},
                )
            }
        }
        compose.onNodeWithText("앨범 열기").assertIsEnabled()
        compose.onNodeWithText("레이아웃 선택").assertIsNotEnabled()
    }

    @Test
    fun recoveryErrorBlocksContinuationEvenAtExactCount() {
        val photos = listOf(
            ImportedPhotoItem("first", "/missing/first.jpg"),
            ImportedPhotoItem("second", "/missing/second.jpg"),
        )
        compose.setContent {
            Pocket4CutTheme {
                PhotoImportContent(
                    frameType = FrameType.TWO_CUT,
                    uiState = PhotoImportUiState(
                        isInitialized = true,
                        photos = photos,
                        message = "복구 오류",
                        isMessageError = true,
                        hasBlockingRecoveryError = true,
                    ),
                    onBack = {}, onOpenAlbum = {}, onMove = { _, _ -> }, onRemove = {},
                    onDismissMessage = {}, onDone = {},
                )
            }
        }
        compose.onNodeWithText("레이아웃 선택").assertIsNotEnabled()
        compose.onNodeWithContentDescription("안내 닫기").assertDoesNotExist()
    }

    @Test
    fun dismissibleErrorNoticeDoesNotBlockExactCount() {
        val photos = listOf(
            ImportedPhotoItem("first", "/missing/first.jpg"),
            ImportedPhotoItem("second", "/missing/second.jpg"),
        )
        compose.setContent {
            Pocket4CutTheme {
                PhotoImportContent(
                    frameType = FrameType.TWO_CUT,
                    uiState = PhotoImportUiState(
                        isInitialized = true,
                        photos = photos,
                        message = "같은 사진은 다시 추가하지 않았습니다.",
                        isMessageError = true,
                    ),
                    onBack = {}, onOpenAlbum = {}, onMove = { _, _ -> }, onRemove = {},
                    onDismissMessage = {}, onDone = {},
                )
            }
        }
        compose.onNodeWithText("레이아웃 선택").assertIsEnabled()
        compose.onNodeWithContentDescription("안내 닫기").assertIsEnabled()
    }

    @Test
    fun photoRowsOfferMoveAndRemoveActionsWithLargeTouchTarget() {
        val photos = listOf(
            ImportedPhotoItem("first", "/missing/first.jpg"),
            ImportedPhotoItem("second", "/missing/second.jpg"),
        )
        compose.setContent {
            Pocket4CutTheme {
                PhotoImportContent(
                    frameType = FrameType.TWO_CUT,
                    uiState = PhotoImportUiState(isInitialized = true, photos = photos),
                    onBack = {}, onOpenAlbum = {}, onMove = { _, _ -> }, onRemove = {},
                    onDismissMessage = {}, onDone = {},
                )
            }
        }
        val first = compose.onNodeWithContentDescription("1번째 가져온 사진")
        val actions = first.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("뒤로 이동", "사진 제거"), actions.map { it.label })
        compose.onNodeWithContentDescription("1번째 사진 제거")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        compose.onNodeWithText("레이아웃 선택").assertIsEnabled()
    }

    @Test
    fun cancelledPickerCanBeReopenedInCompactPortraitWithDoubleFontScale() {
        var opened = 0
        var closed = 0
        compose.setContent {
            CompactPortraitImportViewport {
                PhotoImportContent(
                    frameType = FrameType.TWO_CUT,
                    uiState = PhotoImportUiState(isInitialized = true),
                    onBack = { closed += 1 }, onOpenAlbum = { opened += 1 },
                    onMove = { _, _ -> }, onRemove = {}, onDismissMessage = {}, onDone = {},
                )
            }
        }

        compose.onNodeWithText("앨범 열기")
            .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        compose.onNodeWithText("레이아웃 선택")
            .performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithContentDescription("앨범 사진 확인 닫기")
            .performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, closed) }
    }

    @Test
    fun photoActionsRemainReachableBelowLongNoticeInCompactPortraitWithDoubleFontScale() {
        val photos = List(6) { index -> ImportedPhotoItem("photo-$index", "/missing/photo-$index.jpg") }
        var state by mutableStateOf(PhotoImportUiState(
            isInitialized = true,
            photos = photos.take(1),
            message = List(6) { "일부 사진을 가져오지 못했습니다. 원본을 보존한 채 다시 선택해 주세요." }
                .joinToString("\n"),
            isMessageError = true,
        ))
        var opened = 0
        var completed = 0
        compose.setContent {
            CompactPortraitImportViewport {
                PhotoImportContent(
                    frameType = FrameType.SIX_CUT,
                    uiState = state,
                    onBack = {}, onOpenAlbum = { opened += 1 }, onMove = { _, _ -> },
                    onRemove = {}, onDismissMessage = {}, onDone = { completed += 1 },
                )
            }
        }

        compose.onNodeWithText("사진 추가 · 5장 필요")
            .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        compose.onNodeWithText("레이아웃 선택")
            .performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(photos = photos) }
        compose.onNodeWithContentDescription("6번째 가져온 사진")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("레이아웃 선택")
            .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, completed) }
        compose.onNodeWithContentDescription("안내 닫기")
            .performScrollTo().assertIsDisplayed().assertIsEnabled()
    }

    @Composable
    private fun CompactPortraitImportViewport(content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
            Box(Modifier.size(width = 360.dp, height = 640.dp)) {
                Pocket4CutTheme(content = content)
            }
        }
    }
}
