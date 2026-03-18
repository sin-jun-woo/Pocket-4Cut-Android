package com.pocket4cut.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pocket4cut.presentation.capture.CaptureScreen
import com.pocket4cut.presentation.edit.EditScreen
import com.pocket4cut.presentation.frameTheme.FrameThemeScreen
import com.pocket4cut.presentation.frameTypeSelect.FrameTypeSelectScreen
import com.pocket4cut.presentation.home.HomeScreen
import com.pocket4cut.presentation.result.ResultScreen
import com.pocket4cut.presentation.selection.SelectionScreen

@Composable
fun PocketNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onStart = { navController.navigate(Routes.FRAME_TYPE_SELECT) },
            )
        }

        composable(Routes.FRAME_TYPE_SELECT) {
            FrameTypeSelectScreen(
                onBack = { navController.popBackStack() },
                onSelected = { frameType ->
                    navController.navigate("${Routes.CAPTURE}/${frameType.id}")
                },
            )
        }

        composable(
            route = "${Routes.CAPTURE}/{${Routes.Args.FRAME_TYPE}}",
            arguments = listOf(navArgument(Routes.Args.FRAME_TYPE) { type = NavType.StringType }),
        ) { entry ->
            val frameTypeId = entry.arguments?.getString(Routes.Args.FRAME_TYPE).orEmpty()
            CaptureScreen(
                frameType = FrameType.fromId(frameTypeId),
                onBack = { navController.popBackStack() },
                onCompleted = { sessionId, type ->
                    navController.navigate("${Routes.SELECTION}/${type.id}/$sessionId") {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
            )
        }

        composable(
            route = "${Routes.SELECTION}/{${Routes.Args.FRAME_TYPE}}/{${Routes.Args.SESSION_ID}}",
            arguments = listOf(
                navArgument(Routes.Args.FRAME_TYPE) { type = NavType.StringType },
                navArgument(Routes.Args.SESSION_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val frameTypeId = entry.arguments?.getString(Routes.Args.FRAME_TYPE).orEmpty()
            val sessionId = entry.arguments?.getString(Routes.Args.SESSION_ID).orEmpty()
            SelectionScreen(
                frameType = FrameType.fromId(frameTypeId),
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onDone = { selectedIndexes ->
                    val encoded = NavCodec.encodeIndexes(selectedIndexes)
                    navController.navigate("${Routes.FRAME_THEME}/${frameTypeId}/$sessionId/$encoded")
                },
            )
        }

        composable(
            route = "${Routes.FRAME_THEME}/{${Routes.Args.FRAME_TYPE}}/{${Routes.Args.SESSION_ID}}/{${Routes.Args.SELECTED_INDEXES}}",
            arguments = listOf(
                navArgument(Routes.Args.FRAME_TYPE) { type = NavType.StringType },
                navArgument(Routes.Args.SESSION_ID) { type = NavType.StringType },
                navArgument(Routes.Args.SELECTED_INDEXES) { type = NavType.StringType },
            ),
        ) { entry ->
            val frameTypeId = entry.arguments?.getString(Routes.Args.FRAME_TYPE).orEmpty()
            val sessionId = entry.arguments?.getString(Routes.Args.SESSION_ID).orEmpty()
            val selectedRaw = entry.arguments?.getString(Routes.Args.SELECTED_INDEXES).orEmpty()
            FrameThemeScreen(
                frameType = FrameType.fromId(frameTypeId),
                sessionId = sessionId,
                selectedIndexes = NavCodec.decodeIndexes(selectedRaw),
                onBack = { navController.popBackStack() },
                onDone = { themeId ->
                    navController.navigate("${Routes.EDIT}/${frameTypeId}/$sessionId/$selectedRaw/$themeId")
                },
            )
        }

        composable(
            route = "${Routes.EDIT}/{${Routes.Args.FRAME_TYPE}}/{${Routes.Args.SESSION_ID}}/{${Routes.Args.SELECTED_INDEXES}}/{${Routes.Args.THEME_ID}}",
            arguments = listOf(
                navArgument(Routes.Args.FRAME_TYPE) { type = NavType.StringType },
                navArgument(Routes.Args.SESSION_ID) { type = NavType.StringType },
                navArgument(Routes.Args.SELECTED_INDEXES) { type = NavType.StringType },
                navArgument(Routes.Args.THEME_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val frameTypeId = entry.arguments?.getString(Routes.Args.FRAME_TYPE).orEmpty()
            val sessionId = entry.arguments?.getString(Routes.Args.SESSION_ID).orEmpty()
            val selectedRaw = entry.arguments?.getString(Routes.Args.SELECTED_INDEXES).orEmpty()
            val themeId = entry.arguments?.getString(Routes.Args.THEME_ID).orEmpty()
            EditScreen(
                frameType = FrameType.fromId(frameTypeId),
                sessionId = sessionId,
                selectedIndexes = NavCodec.decodeIndexes(selectedRaw),
                themeId = themeId,
                onBack = { navController.popBackStack() },
                onCompleted = { resultPath ->
                    val encoded = NavCodec.encodePath(resultPath)
                    navController.navigate("${Routes.RESULT}/$encoded")
                },
            )
        }

        composable(
            route = "${Routes.RESULT}/{${Routes.Args.RESULT_PATH}}",
            arguments = listOf(navArgument(Routes.Args.RESULT_PATH) { type = NavType.StringType }),
        ) { entry ->
            val encoded = entry.arguments?.getString(Routes.Args.RESULT_PATH).orEmpty()
            ResultScreen(
                resultPath = NavCodec.decodePath(encoded),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

