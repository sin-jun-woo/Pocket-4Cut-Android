package com.pocket4cut.presentation.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationGuardInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun repeatedSettingsCloseLeavesHomeOnTheBackStack() {
        lateinit var controller: NavHostController
        compose.setContent {
            controller = rememberNavController()
            NavHost(controller, startDestination = Routes.HOME) {
                composable(Routes.HOME) { Text("Home") }
                composable(Routes.SETTINGS) { Text("Settings") }
            }
        }
        compose.runOnIdle { controller.navigate(Routes.SETTINGS) }
        compose.onNodeWithText("Settings").assertIsDisplayed()

        compose.runOnIdle {
            assertTrue(controller.popBackStackIfCurrent(Routes.SETTINGS))
            assertFalse(controller.popBackStackIfCurrent(Routes.SETTINGS))
            assertEquals(Routes.HOME, controller.currentDestination?.route)
        }
        compose.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test fun galleryResumedSelectionReturnsHomeAndRepeatedLeaveCannotPopIt() {
        lateinit var controller: NavHostController
        compose.setContent {
            controller = rememberNavController()
            NavHost(controller, startDestination = Routes.HOME) {
                composable(Routes.HOME) { Text("Home") }
                composable(Routes.GALLERY) { Text("Gallery") }
                composable("${Routes.SELECTION}/{frameType}/{sessionId}") { Text("Selection") }
            }
        }
        compose.runOnIdle {
            controller.navigate(Routes.GALLERY)
            controller.navigate("${Routes.SELECTION}/2/test-session")
        }
        compose.onNodeWithText("Selection").assertIsDisplayed()

        compose.runOnIdle {
            assertTrue(controller.popSelectionToHome())
            assertFalse(controller.popSelectionToHome())
            assertEquals(Routes.HOME, controller.currentDestination?.route)
        }
        compose.onNodeWithText("Home").assertIsDisplayed()
    }
}
