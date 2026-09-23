package com.pocket4cut

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocket4cut.presentation.result.ResultActionControls
import com.pocket4cut.presentation.result.ResultActionState
import com.pocket4cut.presentation.result.ResultLinkLoadState
import com.pocket4cut.presentation.result.ResultOperation
import com.pocket4cut.ui.theme.Pocket4CutTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultActionControlsInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun loadingLinkShowsPreparationAndDisablesShare() {
        setActions(
            ResultActionState(
                linkState = ResultLinkLoadState.LOADING,
                operation = ResultOperation.IDLE,
                isSaved = false,
            ),
        )

        compose.onNodeWithText("결과 준비 중...").assertIsDisplayed()
        compose.onNodeWithText("저장").assertDoesNotExist()
        compose.onNodeWithText("공유").assertIsNotEnabled()
    }

    @Test
    fun readyIdleLinkEnablesSaveAndShare() {
        setActions(
            ResultActionState(
                linkState = ResultLinkLoadState.READY,
                operation = ResultOperation.IDLE,
                isSaved = false,
            ),
        )

        compose.onNodeWithText("저장").assertIsEnabled()
        compose.onNodeWithText("공유").assertIsEnabled()
    }

    @Test
    fun activeSaveDisablesShare() {
        setActions(
            ResultActionState(
                linkState = ResultLinkLoadState.READY,
                operation = ResultOperation.SAVING,
                isSaved = false,
            ),
        )
        compose.onNodeWithText("저장 중...").assertIsDisplayed()
        compose.onNodeWithText("공유").assertIsNotEnabled()
    }

    @Test
    fun activeShareDisablesSaveAndAnotherShare() {
        setActions(
            ResultActionState(
                linkState = ResultLinkLoadState.READY,
                operation = ResultOperation.SHARING,
                isSaved = false,
            ),
        )
        compose.onNodeWithText("저장").assertIsNotEnabled()
        compose.onNodeWithText("공유 중...").assertIsNotEnabled()
    }

    private fun setActions(state: ResultActionState) {
        compose.setContent {
            Pocket4CutTheme {
                ResultActionControls(state = state, onSave = {}, onShare = {})
            }
        }
    }
}
