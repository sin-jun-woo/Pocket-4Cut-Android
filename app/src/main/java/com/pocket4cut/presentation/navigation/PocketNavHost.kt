package com.pocket4cut.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import android.graphics.Bitmap
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.presentation.capture.CaptureScreen
import com.pocket4cut.presentation.detailEdit.DetailEditScreen
import com.pocket4cut.presentation.edit.EditScreen
import com.pocket4cut.presentation.edit.PendingCollageStore
import com.pocket4cut.frame.CustomFrameDesign
import com.pocket4cut.frame.SeasonBackgroundFrameFactory
import com.pocket4cut.presentation.frameFlow.ColorFramePalettePickScreen
import com.pocket4cut.presentation.frameFlow.CustomFrameEditorScreen
import com.pocket4cut.presentation.frameFlow.FrameFlowCoordinatorScreen
import com.pocket4cut.presentation.frameFlow.SeasonBackgroundFramePickScreen
import com.pocket4cut.presentation.frameTypeSelect.FrameTypeSelectScreen
import com.pocket4cut.presentation.gallery.GalleryScreen
import com.pocket4cut.presentation.home.HomeScreen
import com.pocket4cut.presentation.launch.LaunchScreen
import com.pocket4cut.presentation.layoutSelection.LayoutSelectionScreen
import com.pocket4cut.presentation.result.ResultScreen
import com.pocket4cut.presentation.selection.SelectionScreen
import com.pocket4cut.presentation.settings.ContactFeedbackScreen
import com.pocket4cut.presentation.settings.PrivacyPolicyScreen
import com.pocket4cut.presentation.settings.SettingsScreen

@Composable
fun PocketNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LAUNCH,
        modifier = modifier,
    ) {
        // Launch
        composable(Routes.LAUNCH) {
            LaunchScreen(
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LAUNCH) { inclusive = true }
                    }
                },
            )
        }

        // Home
        composable(Routes.HOME) {
            HomeScreen(
                onStart = { navController.navigate(Routes.FRAME_TYPE_SELECT) },
                onGallery = { navController.navigate(Routes.GALLERY) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        // Settings
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onPrivacyPolicy = { navController.navigate(Routes.PRIVACY_POLICY) },
                onContactFeedback = { navController.navigate(Routes.CONTACT_FEEDBACK) },
            )
        }

        composable(Routes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CONTACT_FEEDBACK) {
            ContactFeedbackScreen(onBack = { navController.popBackStack() })
        }

        // Gallery
        composable(Routes.GALLERY) {
            GalleryScreen(
                onBack = { navController.popBackStack() },
                onOpen = { resultPath ->
                    val encoded = NavCodec.encodePath(resultPath)
                    navController.navigate("${Routes.RESULT}/$encoded")
                },
            )
        }

        // Frame Type Select
        composable(Routes.FRAME_TYPE_SELECT) {
            FrameTypeSelectScreen(
                onBack = { navController.popBackStack() },
                onSelected = { frameType ->
                    navController.navigate("${Routes.CAPTURE}/${frameType.id}")
                },
            )
        }

        // Capture
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

        // Selection
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
                    navController.navigate("${Routes.LAYOUT_SELECTION}/${frameTypeId}/$sessionId/$encoded")
                },
            )
        }

        // Layout Selection → now navigates to Frame Theme Select
        composable(
            route = "${Routes.LAYOUT_SELECTION}/{${Routes.Args.FRAME_TYPE}}/{${Routes.Args.SESSION_ID}}/{${Routes.Args.SELECTED_INDEXES}}",
            arguments = listOf(
                navArgument(Routes.Args.FRAME_TYPE) { type = NavType.StringType },
                navArgument(Routes.Args.SESSION_ID) { type = NavType.StringType },
                navArgument(Routes.Args.SELECTED_INDEXES) { type = NavType.StringType },
            ),
        ) { entry ->
            val frameTypeId = entry.arguments?.getString(Routes.Args.FRAME_TYPE).orEmpty()
            val sessionId = entry.arguments?.getString(Routes.Args.SESSION_ID).orEmpty()
            val selectedRaw = entry.arguments?.getString(Routes.Args.SELECTED_INDEXES).orEmpty()
            val frameType = FrameType.fromId(frameTypeId)
            val storage = FileImageStorage(LocalContext.current)
            val selectedIndexes = NavCodec.decodeIndexes(selectedRaw)

            var photoPaths by remember { mutableStateOf(emptyList<String>()) }
            LaunchedEffect(sessionId) {
                val allPaths = storage.getCapturePaths(sessionId)
                photoPaths = selectedIndexes.mapNotNull { idx -> allPaths.getOrNull(idx) }
            }

            LayoutSelectionScreen(
                selectedPhotoPaths = photoPaths,
                requiredCount = frameType.selectCount,
                frameType = frameType,
                onSelectLayout = { layoutId ->
                    navController.navigate(
                        "${Routes.FRAME_THEME_SELECT}/${frameTypeId}/$sessionId/$selectedRaw/${layoutId.name}",
                    )
                },
                onCancel = { navController.popBackStack() },
            )
        }

        // Frame Theme Select
        composable(
            route = "${Routes.FRAME_THEME_SELECT}/{${Routes.Args.FRAME_TYPE}}/{${Routes.Args.SESSION_ID}}/{${Routes.Args.SELECTED_INDEXES}}/{${Routes.Args.LAYOUT_ID}}",
            arguments = listOf(
                navArgument(Routes.Args.FRAME_TYPE) { type = NavType.StringType },
                navArgument(Routes.Args.SESSION_ID) { type = NavType.StringType },
                navArgument(Routes.Args.SELECTED_INDEXES) { type = NavType.StringType },
                navArgument(Routes.Args.LAYOUT_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val frameTypeId = entry.arguments?.getString(Routes.Args.FRAME_TYPE).orEmpty()
            val sessionId = entry.arguments?.getString(Routes.Args.SESSION_ID).orEmpty()
            val selectedRaw = entry.arguments?.getString(Routes.Args.SELECTED_INDEXES).orEmpty()
            val layoutIdStr = entry.arguments?.getString(Routes.Args.LAYOUT_ID).orEmpty()
            val frameType = FrameType.fromId(frameTypeId)
            val storage = FileImageStorage(LocalContext.current)
            val selectedIndexes = NavCodec.decodeIndexes(selectedRaw)
            val frameLayoutId = remember(layoutIdStr) {
                runCatching { FrameLayoutId.valueOf(layoutIdStr) }.getOrElse { FrameLayoutId.FOUR_VERTICAL }
            }
            val frameStyle = remember(frameLayoutId) { FrameLayouts.byId(frameLayoutId) }

            var photoPaths by remember { mutableStateOf(emptyList<String>()) }
            LaunchedEffect(sessionId) {
                val allPaths = storage.getCapturePaths(sessionId)
                photoPaths = selectedIndexes.mapNotNull { idx -> allPaths.getOrNull(idx) }
            }

            val theme = remember(frameType) { FrameCatalog.themes(frameType).first() }

            var flowStep by remember { mutableStateOf("choose") }

            when (flowStep) {
                "choose" -> FrameFlowCoordinatorScreen(
                    onColorPick = { flowStep = "color" },
                    onSeasonPick = { flowStep = "season" },
                    onCustomEditor = { flowStep = "custom" },
                    onDismiss = { navController.popBackStack() },
                )
                "color" -> ColorFramePalettePickScreen(
                    images = photoPaths.mapNotNull { path ->
                        com.pocket4cut.core.util.BitmapDecoding.decodeSampled(path, 512)
                    },
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    onBack = { flowStep = "choose" },
                    onDismiss = { navController.popBackStack() },
                    onCompleted = { color ->
                        navController.navigate(
                            "${Routes.EDIT}/${frameTypeId}/$sessionId/$selectedRaw/$layoutIdStr/${theme.id}",
                        )
                    },
                )
                "season" -> SeasonBackgroundFramePickScreen(
                    images = photoPaths.mapNotNull { path ->
                        com.pocket4cut.core.util.BitmapDecoding.decodeSampled(path, 512)
                    },
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    onBack = { flowStep = "choose" },
                    onDismiss = { navController.popBackStack() },
                    onCompleted = { season ->
                        navController.navigate(
                            "${Routes.EDIT}/${frameTypeId}/$sessionId/$selectedRaw/$layoutIdStr/${theme.id}",
                        )
                    },
                )
                "custom" -> CustomFrameEditorScreen(
                    images = photoPaths.mapNotNull { path ->
                        com.pocket4cut.core.util.BitmapDecoding.decodeSampled(path, 512)
                    },
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    onBack = { flowStep = "choose" },
                    onDismiss = { navController.popBackStack() },
                    onCompleted = { design ->
                        navController.navigate(
                            "${Routes.EDIT}/${frameTypeId}/$sessionId/$selectedRaw/$layoutIdStr/${theme.id}",
                        )
                    },
                )
            }
        }

        // Edit (now receives themeId)
        composable(
            route = "${Routes.EDIT}/{${Routes.Args.FRAME_TYPE}}/{${Routes.Args.SESSION_ID}}/{${Routes.Args.SELECTED_INDEXES}}/{${Routes.Args.LAYOUT_ID}}/{${Routes.Args.THEME_ID}}",
            arguments = listOf(
                navArgument(Routes.Args.FRAME_TYPE) { type = NavType.StringType },
                navArgument(Routes.Args.SESSION_ID) { type = NavType.StringType },
                navArgument(Routes.Args.SELECTED_INDEXES) { type = NavType.StringType },
                navArgument(Routes.Args.LAYOUT_ID) { type = NavType.StringType },
                navArgument(Routes.Args.THEME_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val frameTypeId = entry.arguments?.getString(Routes.Args.FRAME_TYPE).orEmpty()
            val sessionId = entry.arguments?.getString(Routes.Args.SESSION_ID).orEmpty()
            val selectedRaw = entry.arguments?.getString(Routes.Args.SELECTED_INDEXES).orEmpty()
            val layoutId = entry.arguments?.getString(Routes.Args.LAYOUT_ID).orEmpty()
            val themeId = entry.arguments?.getString(Routes.Args.THEME_ID).orEmpty()
            EditScreen(
                frameType = FrameType.fromId(frameTypeId),
                sessionId = sessionId,
                selectedIndexes = NavCodec.decodeIndexes(selectedRaw),
                layoutId = layoutId,
                themeId = themeId,
                onBack = { navController.popBackStack() },
                onContinueToDetailEdit = {
                    navController.navigate("${Routes.DETAIL_EDIT}/${frameTypeId}/$sessionId/$selectedRaw/$layoutId/$themeId")
                },
                onComplete = { resultPath ->
                    val encoded = NavCodec.encodePath(resultPath)
                    navController.navigate("${Routes.RESULT}/$encoded")
                },
            )
        }

        // Detail Edit
        composable(
            route = "${Routes.DETAIL_EDIT}/{${Routes.Args.FRAME_TYPE}}/{${Routes.Args.SESSION_ID}}/{${Routes.Args.SELECTED_INDEXES}}/{${Routes.Args.LAYOUT_ID}}/{${Routes.Args.THEME_ID}}",
            arguments = listOf(
                navArgument(Routes.Args.FRAME_TYPE) { type = NavType.StringType },
                navArgument(Routes.Args.SESSION_ID) { type = NavType.StringType },
                navArgument(Routes.Args.SELECTED_INDEXES) { type = NavType.StringType },
                navArgument(Routes.Args.LAYOUT_ID) { type = NavType.StringType },
                navArgument(Routes.Args.THEME_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val frameTypeId = entry.arguments?.getString(Routes.Args.FRAME_TYPE).orEmpty()
            val sessionId = entry.arguments?.getString(Routes.Args.SESSION_ID).orEmpty()
            val selectedRaw = entry.arguments?.getString(Routes.Args.SELECTED_INDEXES).orEmpty()
            val layoutIdStr = entry.arguments?.getString(Routes.Args.LAYOUT_ID).orEmpty()
            val themeId = entry.arguments?.getString(Routes.Args.THEME_ID).orEmpty()
            val frameType = FrameType.fromId(frameTypeId)
            val frameLayoutId = runCatching { FrameLayoutId.valueOf(layoutIdStr) }.getOrElse { FrameLayoutId.FOUR_VERTICAL }
            val frameStyle = FrameLayouts.byId(frameLayoutId)
            val theme = FrameCatalog.themes(frameType).firstOrNull { it.id == themeId }
                ?: FrameCatalog.themes(frameType).first()
            val selectedIndexes = NavCodec.decodeIndexes(selectedRaw)

            val context = LocalContext.current
            val storage = FileImageStorage(context)
            var photoBitmaps by remember { mutableStateOf(emptyList<Bitmap>()) }
            var photoPaths by remember { mutableStateOf(emptyList<String>()) }
            LaunchedEffect(sessionId) {
                val allPaths = storage.getCapturePaths(sessionId)
                val selected = selectedIndexes.mapNotNull { idx -> allPaths.getOrNull(idx) }
                photoPaths = selected
                photoBitmaps = selected.mapNotNull { path ->
                    com.pocket4cut.core.util.BitmapDecoding.decodeSampled(path, 2048)
                }
            }

            if (photoBitmaps.isNotEmpty()) {
                val pending = PendingCollageStore.read(context, sessionId)
                DetailEditScreen(
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    frameColor = FrameColors.byId(pending?.frameColorId ?: "white"),
                    orderedImages = photoBitmaps,
                    imagePaths = photoPaths,
                    sessionId = sessionId,
                    selectedIndexes = selectedIndexes,
                    globalFilter = pending?.filterId ?: FilterId.ORIGINAL,
                    customText = pending?.text.orEmpty(),
                    showDate = pending?.showDate ?: true,
                    textFontSize = pending?.textFontSize ?: 16f,
                    dateFontSize = pending?.dateFontSize ?: 16f,
                    captionFontName = pending?.captionFontName,
                    captionColorRGB = pending?.captionColorRGB,
                    onBack = { navController.popBackStack() },
                    onResult = { resultPath ->
                        val encoded = NavCodec.encodePath(resultPath)
                        navController.navigate("${Routes.RESULT}/$encoded")
                    },
                )
            }
        }

        // Result
        composable(
            route = "${Routes.RESULT}/{${Routes.Args.RESULT_PATH}}",
            arguments = listOf(navArgument(Routes.Args.RESULT_PATH) { type = NavType.StringType }),
        ) { entry ->
            val encoded = entry.arguments?.getString(Routes.Args.RESULT_PATH).orEmpty()
            ResultScreen(
                resultPath = NavCodec.decodePath(encoded),
                onHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
