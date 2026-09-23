package com.pocket4cut

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
                    ),
                    onBack = {}, onOpenAlbum = {}, onMove = { _, _ -> }, onRemove = {},
                    onDismissMessage = {}, onDone = {},
                )
            }
        }
        compose.onNodeWithText("레이아웃 선택").assertIsNotEnabled()
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
}
