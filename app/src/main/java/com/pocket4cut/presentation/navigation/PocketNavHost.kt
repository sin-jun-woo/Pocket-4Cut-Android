package com.pocket4cut.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pocket4cut.presentation.capture.CaptureScreen
import com.pocket4cut.presentation.frameTypeSelect.FrameTypeSelectScreen
import com.pocket4cut.presentation.home.HomeScreen
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
            )
        }
    }
}

