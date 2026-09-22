package com.pocket4cut.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionStage
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
            val context = LocalContext.current
            var recentDraft by remember { mutableStateOf<SessionDocument?>(null) }
            var draftLoadError by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                runCatching {
                    SessionDocumentRepository(context).scanForGallery()
                }.onSuccess { scan ->
                    recentDraft = scan.documents.firstOrNull { session ->
                        session.stage != SessionStage.RESULT && session.stage != SessionStage.DELETED &&
                            session.stage != SessionStage.NEEDS_RECOVERY
                    }
                    draftLoadError = if (scan.unreadableSessionIds.isNotEmpty()) {
                        "일부 저장된 작업은 복구가 필요합니다. 보관함에서 확인해 주세요."
                    } else null
                }.onFailure { draftLoadError = "저장된 작업을 읽을 수 없습니다. 보관함에서 확인해 주세요." }
            }
            HomeScreen(
                onStart = { navController.navigate(Routes.FRAME_TYPE_SELECT) },
                onGallery = { navController.navigate(Routes.GALLERY) },
                onResume = recentDraft?.let { draft ->
                    { navController.navigate(resumeRoute(draft)) }
                },
                resumeLabel = recentDraft?.let { "${it.selectedCount}컷 이어서 작업하기" },
                notice = draftLoadError,
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
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            GalleryScreen(
                onBack = { navController.popBackStack() },
                onOpen = { resultPath ->
                    val encoded = NavCodec.encodePath(resultPath)
                    navController.navigate("${Routes.RESULT}/$encoded")
                },
                onOpenResult = { _, _, resultPath ->
                    val encoded = NavCodec.encodePath(resultPath)
                    navController.navigate("${Routes.RESULT}/$encoded")
                },
                onResumeDraft = { sessionId, _ ->
                    scope.launch {
                        val document = SessionDocumentRepository(context).getById(sessionId) ?: return@launch
                        navController.navigate(resumeRoute(document))
                    }
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

        composable(
            route = "${Routes.CAPTURE}/{${Routes.Args.FRAME_TYPE}}/{${Routes.Args.SESSION_ID}}",
            arguments = listOf(
                navArgument(Routes.Args.FRAME_TYPE) { type = NavType.StringType },
                navArgument(Routes.Args.SESSION_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val frameType = FrameType.fromId(entry.arguments?.getString(Routes.Args.FRAME_TYPE).orEmpty())
            val sessionId = entry.arguments?.getString(Routes.Args.SESSION_ID).orEmpty()
            CaptureScreen(
                frameType = frameType,
                resumeSessionId = sessionId,
                onBack = { navController.popBackStack() },
                onCompleted = { id, type ->
                    navController.navigate("${Routes.SELECTION}/${type.id}/$id") {
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
            val context = LocalContext.current
            val sessions = remember(context) { SessionDocumentRepository(context) }
            val scope = rememberCoroutineScope()

            var photoPaths by remember { mutableStateOf(emptyList<String>()) }
            var layoutSelectionVersion by remember(sessionId) { mutableIntStateOf(2) }
            LaunchedEffect(sessionId) {
                val document = sessions.getById(sessionId) ?: return@LaunchedEffect
                layoutSelectionVersion = document.draft.layoutVersion
                photoPaths = document.draft.selectedPhotoIdsInOrder.map { id ->
                    sessions.resolvePhotoPath(document.photos.first { it.photoId == id }).absolutePath
                }
            }

            LayoutSelectionScreen(
                selectedPhotoPaths = photoPaths,
                requiredCount = frameType.selectCount,
                frameType = frameType,
                layoutVersion = layoutSelectionVersion,
                onSelectLayout = { layoutId ->
                    scope.launch {
                        val doc = sessions.getById(sessionId) ?: return@launch
                        sessions.update(sessionId, doc.revision) { current ->
                            current.copy(stage = SessionStage.FRAME,
                                draft = current.draft.copy(layoutId = layoutId.name))
                        }
                        navController.navigate(
                            "${Routes.FRAME_THEME_SELECT}/${frameTypeId}/$sessionId/$selectedRaw/${layoutId.name}",
                        )
                    }
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
            val context = LocalContext.current
            val sessions = remember(context) { SessionDocumentRepository(context) }
            val scope = rememberCoroutineScope()
            val frameSaveMutex = remember(sessionId) { Mutex() }
            val frameLayoutId = remember(layoutIdStr) {
                runCatching { FrameLayoutId.valueOf(layoutIdStr) }.getOrElse { FrameLayoutId.FOUR_VERTICAL }
            }
            val frameStyle = remember(frameLayoutId) { FrameLayouts.byId(frameLayoutId) }

            var photoPaths by remember { mutableStateOf(emptyList<String>()) }
            LaunchedEffect(sessionId) {
                val document = sessions.getById(sessionId) ?: return@LaunchedEffect
                photoPaths = document.draft.selectedPhotoIdsInOrder.map { id ->
                    sessions.resolvePhotoPath(document.photos.first { it.photoId == id }).absolutePath
                }
            }

            var theme by remember { mutableStateOf(FrameCatalog.themes(frameType).first()) }
            var initialDesign by remember { mutableStateOf<CustomFrameDesign?>(null) }
            var layoutVersion by remember { mutableIntStateOf(2) }

            var flowStep by remember { mutableStateOf("choose") }
            var frameLoaded by remember { mutableStateOf(false) }
            var frameSaveError by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(sessionId) {
                val document = sessions.getById(sessionId) ?: return@LaunchedEffect
                flowStep = document.draft.frameStep
                theme = FrameCatalog.themes(frameType).firstOrNull { it.id == document.draft.themeId }
                    ?: FrameCatalog.themes(frameType).first()
                initialDesign = document.draft.customDesignJson?.let(PendingCollageStore::deserializeDesign)
                layoutVersion = document.draft.layoutVersion
                frameLoaded = true
            }

            fun saveFrame(step: String, onSaved: () -> Unit = {}, onFailed: () -> Unit = {},
                          transform: (com.pocket4cut.domain.model.SessionDraft) -> com.pocket4cut.domain.model.SessionDraft = { it }) {
                scope.launch {
                    try {
                        frameSaveMutex.withLock {
                            val document = sessions.getById(sessionId)
                                ?: error("편집할 작업을 찾지 못했습니다.")
                            sessions.update(sessionId, document.revision) { current ->
                                current.copy(stage = if (step == "edit") SessionStage.EDIT else SessionStage.FRAME,
                                    draft = transform(current.draft).copy(themeId = theme.id,
                                        frameStep = if (step == "edit") current.draft.frameStep else step))
                            }
                        }
                        frameSaveError = null
                        onSaved()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        frameSaveError = failure.message ?: "프레임을 저장하지 못했습니다. 다시 시도해 주세요."
                        onFailed()
                    }
                }
            }

            var previewBitmaps by remember { mutableStateOf(emptyList<Bitmap>()) }
            LaunchedEffect(photoPaths) {
                previewBitmaps = photoPaths.mapNotNull { path ->
                    com.pocket4cut.core.util.BitmapDecoding.decodeSampled(path, 512)
                }
            }

            fun navigateToEdit() {
                navController.navigate(
                    "${Routes.EDIT}/${frameTypeId}/$sessionId/$selectedRaw/$layoutIdStr/${theme.id}",
                )
            }

            if (frameLoaded) when (flowStep) {
                "choose" -> FrameFlowCoordinatorScreen(
                    onColorPick = { saveFrame("color", onSaved = { flowStep = "color" }) },
                    onSeasonPick = { saveFrame("season", onSaved = { flowStep = "season" }) },
                    onCustomEditor = { saveFrame("custom", onSaved = { flowStep = "custom" }) },
                    onDismiss = { navController.popBackStack() },
                )
                "color" -> ColorFramePalettePickScreen(
                    images = previewBitmaps,
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    layoutVersion = layoutVersion,
                    onBack = { saveFrame("choose", onSaved = { flowStep = "choose" }) },
                    onDismiss = { navController.popBackStack() },
                    onCompleted = { color ->
                        saveFrame("edit", onSaved = { navigateToEdit() }) {
                            it.copy(frameColorId = color.id, backgroundType = "solid",
                                seasonId = null, customDesignJson = null)
                        }
                    },
                )
                "season" -> SeasonBackgroundFramePickScreen(
                    images = previewBitmaps,
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    layoutVersion = layoutVersion,
                    onBack = { saveFrame("choose", onSaved = { flowStep = "choose" }) },
                    onDismiss = { navController.popBackStack() },
                    onCompleted = { season ->
                        val design = SeasonBackgroundFrameFactory.design(season)
                        saveFrame("edit", onSaved = { navigateToEdit() }) {
                            it.copy(backgroundType = "season", seasonId = season.name.lowercase(),
                                customDesignJson = PendingCollageStore.serializeDesign(design))
                        }
                    },
                )
                "custom" -> CustomFrameEditorScreen(
                    images = previewBitmaps,
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    layoutVersion = layoutVersion,
                    onBack = { design, failed ->
                        saveFrame("choose", onSaved = {
                            initialDesign = design
                            flowStep = "choose"
                        }, onFailed = failed) {
                            it.copy(customDesignJson = PendingCollageStore.serializeDesign(design),
                                frameColorId = design.fillColorId, backgroundType = "solid")
                        }
                    },
                    onDismiss = { design, failed ->
                        saveFrame("custom", onSaved = { navController.popBackStack() }, onFailed = failed) {
                            it.copy(customDesignJson = PendingCollageStore.serializeDesign(design),
                                frameColorId = design.fillColorId, backgroundType = "solid")
                        }
                    },
                    initialDesign = initialDesign,
                    saveError = frameSaveError,
                    onDraftChanged = { design ->
                        saveFrame("custom") { it.copy(customDesignJson = PendingCollageStore.serializeDesign(design),
                            frameColorId = design.fillColorId, backgroundType = "solid") }
                    },
                    onCompleted = { design, failed ->
                        saveFrame("edit", onSaved = { navigateToEdit() }, onFailed = failed) {
                            it.copy(frameColorId = design.fillColorId, backgroundType = "solid",
                                seasonId = null, customDesignJson = PendingCollageStore.serializeDesign(design))
                        }
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
                    navController.navigate("${Routes.RESULT}/$encoded?auto=true")
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
            val sessions = remember(context) { SessionDocumentRepository(context) }
            var photoBitmaps by remember { mutableStateOf(emptyList<Bitmap>()) }
            var photoPaths by remember { mutableStateOf(emptyList<String>()) }
            var photoIds by remember { mutableStateOf(emptyList<String>()) }
            var detailDocument by remember { mutableStateOf<SessionDocument?>(null) }
            LaunchedEffect(sessionId) {
                val document = sessions.getById(sessionId) ?: return@LaunchedEffect
                val refs = document.draft.selectedPhotoIdsInOrder.map { id ->
                    document.photos.firstOrNull { it.photoId == id } ?: return@LaunchedEffect
                }
                val paths = refs.map { sessions.resolvePhotoPath(it).absolutePath }
                detailDocument = document
                photoIds = refs.map { it.photoId }
                photoPaths = paths
                photoBitmaps = paths.mapNotNull { path ->
                    com.pocket4cut.core.util.BitmapDecoding.decodeSampled(path, 1024)
                }
            }

            if (photoBitmaps.size == photoIds.size && photoBitmaps.isNotEmpty() && detailDocument != null) {
                val draft = detailDocument!!.draft
                val design = draft.customDesignJson?.let { PendingCollageStore.deserializeDesign(it) }
                DetailEditScreen(
                    frameType = frameType,
                    frameStyle = frameStyle,
                    theme = theme,
                    frameColor = FrameColors.byId(draft.frameColorId),
                    orderedImages = photoBitmaps,
                    imagePaths = photoPaths,
                    photoIds = photoIds,
                    initialAdjustments = draft.adjustmentsByPhotoId,
                    layoutVersion = draft.layoutVersion,
                    dateText = draft.dateText,
                    sessionId = sessionId,
                    selectedIndexes = selectedIndexes,
                    globalFilter = runCatching { FilterId.valueOf(draft.filterId) }.getOrDefault(FilterId.ORIGINAL),
                    customText = draft.caption,
                    showDate = draft.showDate,
                    textFontSize = draft.textFontSize,
                    dateFontSize = draft.dateFontSize,
                    captionFontName = draft.captionFontName,
                    captionColorRGB = draft.captionColorRgb,
                    customFrameDesign = design,
                    onBack = { navController.popBackStack() },
                    onResult = { resultPath ->
                        val encoded = NavCodec.encodePath(resultPath)
                        navController.navigate("${Routes.RESULT}/$encoded?auto=true")
                    },
                )
            }
        }

        // Result
        composable(
            route = "${Routes.RESULT}/{${Routes.Args.RESULT_PATH}}?auto={auto}",
            arguments = listOf(
                navArgument(Routes.Args.RESULT_PATH) { type = NavType.StringType },
                navArgument("auto") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val encoded = entry.arguments?.getString(Routes.Args.RESULT_PATH).orEmpty()
            ResultScreen(
                resultPath = NavCodec.decodePath(encoded),
                autoSave = entry.arguments?.getBoolean("auto") == true,
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

private fun resumeRoute(document: SessionDocument): String {
    val frameType = FrameType.entries.firstOrNull { it.captureCount == document.captureCount }
        ?: FrameType.FOUR_CUT
    val id = document.sessionId
    if (document.stage == SessionStage.CAPTURE) return "${Routes.CAPTURE}/${frameType.id}/$id"
    if (document.stage == SessionStage.SELECT) return "${Routes.SELECTION}/${frameType.id}/$id"
    val orderedPhotos = document.photos.sortedBy { it.captureIndex }
    val selected = document.draft.selectedPhotoIdsInOrder.mapNotNull { photoId ->
        orderedPhotos.indexOfFirst { it.photoId == photoId }.takeIf { it >= 0 }
    }
    if (selected.size != document.selectedCount) return "${Routes.SELECTION}/${frameType.id}/$id"
    val selectedRaw = NavCodec.encodeIndexes(selected)
    if (document.draft.layoutId.isBlank()) {
        return "${Routes.LAYOUT_SELECTION}/${frameType.id}/$id/$selectedRaw"
    }
    val layout = document.draft.layoutId
    if (document.stage == SessionStage.FRAME) {
        return "${Routes.FRAME_THEME_SELECT}/${frameType.id}/$id/$selectedRaw/$layout"
    }
    val theme = document.draft.themeId.takeIf { it.isNotBlank() }
        ?: FrameCatalog.themes(frameType).first().id
    val destination = if (document.stage == SessionStage.DETAIL) Routes.DETAIL_EDIT else Routes.EDIT
    return "$destination/${frameType.id}/$id/$selectedRaw/$layout/$theme"
}
