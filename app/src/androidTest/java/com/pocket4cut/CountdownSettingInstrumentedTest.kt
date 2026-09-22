package com.pocket4cut

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.presentation.settings.AppSettings
import com.pocket4cut.presentation.settings.SettingsScreen
import com.pocket4cut.ui.theme.Pocket4CutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CountdownSettingInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun countdownValuePersistsAndReappearsAfterLeavingSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppSettings.init(context)
        val original = AppSettings.countdownSeconds
        try {
            AppSettings.updateCountdownSeconds(3)
            var showSettings by mutableStateOf(true)
            compose.setContent {
                Pocket4CutTheme {
                    if (showSettings) {
                        SettingsScreen(
                            onBack = { showSettings = false },
                            onPrivacyPolicy = {},
                            onContactFeedback = {},
                        )
                    }
                }
            }

            compose.onNodeWithContentDescription("카운트다운 시간")
                .performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(1f)) }
            compose.runOnIdle {
                assertEquals(1, AppSettings.countdownSeconds)
                assertEquals(
                    1,
                    context.getSharedPreferences("pocket4cut_settings", Context.MODE_PRIVATE)
                        .getInt("countdownSeconds", -1),
                )
                showSettings = false
            }
            compose.runOnIdle { showSettings = true }
            compose.onNodeWithText("1초").assertExists()
        } finally {
            AppSettings.updateCountdownSeconds(original)
        }
    }
}
