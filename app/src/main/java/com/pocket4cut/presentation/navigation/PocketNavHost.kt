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
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.data.importing.PhotoImportRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.domain.model.InputSource
import com.pocket4cut.domain.model.PhotoAdjustments
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.PhotoCropTransform
import com.pocket4cut.frame.PhotoEditPipeline
import com.pocket4cut.frame.occasion.OccasionCatalogContract
import com.pocket4cut.frame.occasion.OccasionCatalogLoader
import com.pocket4cut.frame.occasion.OccasionTheme
import com.pocket4cut.presentation.capture.CaptureScreen
import com.pocket4cut.presentation.detailEdit.DetailEditScreen
import com.pocket4cut.presentation.edit.EditScreen
import com.pocket4cut.presentation.edit.PendingCollageStore
import com.pocket4cut.frame.CustomFrameDesign
import com.pocket4cut.frame.SeasonBackgroundFrameFactory
import com.pocket4cut.presentation.frameFlow.ColorFramePalettePickScreen
import com.pocket4cut.presentation.frameFlow.CustomFrameEditorScreen
import com.pocket4cut.presentation.frameFlow.FrameFlowCoordinatorScreen
import com.pocket4cut.presentation.frameFlow.OccasionFramePickRoute
import com.pocket4cut.presentation.frameFlow.SeasonBackgroundFramePickScreen
import com.pocket4cut.presentation.frameTypeSelect.FrameTypeSelectScreen
import com.pocket4cut.presentation.gallery.GalleryScreen
import com.pocket4cut.presentation.home.HomeScreen
import com.pocket4cut.presentation.launch.LaunchScreen
import com.pocket4cut.presentation.layoutSelection.LayoutSelectionScreen
import com.pocket4cut.presentation.photoImport.PhotoImportScreen
import com.pocket4cut.presentation.result.ResultScreen
import com.pocket4cut.presentation.selection.SelectionScreen
import com.pocket4cut.presentation.settings.ContactFeedbackScreen
import com.pocket4cut.presentation.settings.PrivacyPolicyScreen
import com.pocket4cut.presentation.settings.SettingsScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
            var galleryCount by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                runCatching {
                    val importRecoveries = PhotoImportRepository(context).recoverAll()
                    SessionDocumentRepository(context).scanForGallery() to importRecoveries
                }.onSuccess { (scan, importRecoveries) ->
                    galleryCount = scan.documents.sumOf { it.results.size }
                    recentDraft = scan.documents.firstOrNull { session ->
                        session.stage != SessionStage.RESULT && session.stage != SessionStage.DELETED &&
                            session.stage != SessionStage.NEEDS_RECOVERY
                    }
                    draftLoadError = when {
                        importRecoveries.any { it.failures.isNotEmpty() || it.needsReselection } ->
                            "일부 앨범 가져오기를 복구하지 못했습니다. 작업 보관함에서 확인해 주세요."
                        scan.legacyMigrationError != null ->
                            "이전 버전의 저장 기록을 읽지 못했습니다. 원본은 보존되어 있으며 작업 보관함에서 읽을 수 있는 작업을 확인할 수 있습니다."
                        scan.unreadableSessionIds.isNotEmpty() ->
                            "일부 저장된 작업은 복구가 필요합니다. 작업 보관함에서 확인해 주세요."
                        else -> null
                    }
                }.onFailure { draftLoadError = "저장된 작업을 읽을 수 없습니다. 작업 보관함에서 확인해 주세요." }
            }
            HomeScreen(
                onCamera = {
                    navController.navigate("${Routes.FRAME_TYPE_SELECT}/${InputSource.CAMERA.name}")
                },
                onAlbum = {
                    navController.navigate("${Routes.FRAME_TYPE_SELECT}/${InputSource.ALBUM.name}")
                },
                onGallery = { navController.navigate(Routes.GALLERY) },
                onResume = recentDraft?.let { draft ->
                    { navController.navigate(resumeRoute(draft)) }
                },
                resumeLabel = recentDraft?.let {
                    val source = if (it.inputSource == InputSource.ALBUM) "앨범" else "카메라"
                    "$source ${it.selectedCount}컷 이어서 작업하기"
                },
                notice = draftLoadError,
                onSettings = { navController.navigate(Routes.SETTINGS) },
                galleryCount = galleryCount,
            )
        }

        // Settings
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStackIfCurrent(Routes.SETTINGS) },
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
        composable(
            route = "${Routes.FRAME_TYPE_SELECT}/{${Routes.Args.INPUT_SOURCE}}",
            arguments = listOf(navArgument(Routes.Args.INPUT_SOURCE) { type = NavType.StringType }),
        ) { entry ->
            val inputSource = runCatching {
                InputSource.valueOf(entry.arguments?.getString(Routes.Args.INPUT_SOURCE).orEmpty())
            }.getOrDefault(InputSource.CAMERA)
            FrameTypeSelectScreen(
                inputSource = inputSource,
                onBack = { navController.popBackStack() },
                onSelected = { frameType ->
                    val destination = when (inputSource) {
                        InputSource.CAMERA -> "${Routes.CAPTURE}/${frameType.id}"
                        InputSource.ALBUM -> "${Routes.PHOTO_IMPORT}/${frameType.id}"
                    }
                    navController.navigate(destination)
                },
            )
        }

        // Album import. A route with session ID is used only when resuming a saved import draft.
        composable(
            route = "${Routes.PHOTO_IMPORT}/{${Routes.Args.FRAME_TYPE}}",
            arguments = listOf(navArgument(Routes.Args.FRAME_TYPE) { type = NavType.StringType }),
        ) { entry ->
            val frameType = FrameType.fromId(entry.arguments?.getString(Routes.Args.FRAME_TYPE).orEmpty())
            PhotoImportScreen(
                frameType = frameType,
                resumeSessionId = null,
                onBack = { navController.popBackStack() },
                onDone = { sessionId ->
                    val selected = NavCodec.encodeIndexes((0 until frameType.selectCount).toList())
                    navController.navigate("${Routes.LAYOUT_SELECTION}/${frameType.id}/$sessionId/$selected")
                },
            )
        }

        composable(
            route = "${Routes.PHOTO_IMPORT}/{${Routes.Args.FRAME_TYPE}}/{${Routes.Args.SESSION_ID}}",
            arguments = listOf(
                navArgument(Routes.Args.FRAME_TYPE) { type = NavType.StringType },
                navArgument(Routes.Args.SESSION_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val frameType = FrameType.fromId(entry.arguments?.getString(Routes.Args.FRAME_TYPE).orEmpty())
            val sessionId = entry.arguments?.getString(Routes.Args.SESSION_ID).orEmpty()
            PhotoImportScreen(
                frameType = frameType,
                resumeSessionId = sessionId,
                onBack = { navController.popBackStack() },
                onDone = { id ->
                    val selected = NavCodec.encodeIndexes((0 until frameType.selectCount).toList())
                    navController.navigate("${Routes.LAYOUT_SELECTION}/${frameType.id}/$id/$selected")
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
                onBack = { navController.popSelectionToHome() },
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

            var previewBitmaps by remember { mutableStateOf(emptyList<Bitmap>()) }
            var previewCropTransforms by remember { mutableStateOf(emptyList<PhotoCropTransform>()) }
            var previewFilterId by remember { mutableStateOf(FilterId.ORIGINAL) }
            var previewCaptionText by remember { mutableStateOf<String?>(null) }
            var previewCaptionDate by remember { mutableStateOf<String?>(null) }
            var previewTextSize by remember { mutableStateOf(16f) }
            var previewDateSize by remember { mutableStateOf(16f) }
            var previewFontName by remember { mutableStateOf<String?>(null) }
            var previewColorRgb by remember { mutableStateOf<Long?>(null) }

            var theme by remember { mutableStateOf(FrameCatalog.themes(frameType).first()) }
            var initialDesign by remember { mutableStateOf<CustomFrameDesign?>(null) }
            var layoutVersion by remember { mutableIntStateOf(2) }

            var flowStep by remember { mutableStateOf("choose") }
            var frameLoaded by remember { mutableStateOf(false) }
            var frameNeedsRecovery by remember { mutableStateOf(false) }
            var frameLoadError by remember { mutableStateOf<String?>(null) }
            var frameRetryKey by remember { mutableIntStateOf(0) }
            var frameSaveError by remember { mutableStateOf<String?>(null) }
            var initialOccasionThemeId by remember { mutableStateOf<String?>(null) }
            var occasionApplying by remember { mutableStateOf(false) }
            LaunchedEffect(sessionId, frameRetryKey) {
                frameLoaded = false
                frameNeedsRecovery = false
                frameLoadError = null
                val document = try {
                    sessions.getById(sessionId)
                } catch (failure: Exception) {
                    frameLoadError = failure.message ?: "저장된 작업을 읽지 못했습니다."
                    frameLoaded = true
                    return@LaunchedEffect
                } ?: run {
                    frameNeedsRecovery = true
                    frameLoaded = true
                    return@LaunchedEffect
                }
                if (document.stage == SessionStage.NEEDS_RECOVERY) {
                    frameNeedsRecovery = true
                    frameLoaded = true
                    return@LaunchedEffect
                }
                flowStep = document.draft.frameStep
                theme = FrameCatalog.themes(frameType).firstOrNull { it.id == document.draft.themeId }
                    ?: FrameCatalog.themes(frameType).first()
                initialDesign = document.draft.customDesignJson?.let(PendingCollageStore::deserializeDesign)
                initialOccasionThemeId = document.draft.occasionThemeId
                layoutVersion = document.draft.layoutVersion
                val preview = try {
                    loadFramePreview(sessions, document, reqSize = 512)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Throwable) {
                    frameLoadError = failure.message ?: "사진 미리보기를 준비하지 못했습니다."
                    frameLoaded = true
                    return@LaunchedEffect
                }
                previewBitmaps = preview.images
                previewCropTransforms = preview.cropTransforms
                previewFilterId = preview.filterId
                previewCaptionText = preview.captionTextPart
                previewCaptionDate = preview.captionDatePart
                previewTextSize = preview.captionTextSizePt
                previewDateSize = preview.captionDateSizePt
                previewFontName = preview.captionFontName
                previewColorRgb = preview.captionColorRgb
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
                                    draft = transform(current.draft).copy(
                                        themeId = if (step == "occasion") current.draft.themeId else theme.id,
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

            fun navigateToEdit() {
                navController.navigate(
                    "${Routes.EDIT}/${frameTypeId}/$sessionId/$selectedRaw/$layoutIdStr/${theme.id}",
                )
            }

            if (frameLoaded && frameNeedsRecovery) {
                SessionRecoveryRequiredScreen(
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onBack = { navController.popBackStack() },
                )
            } else if (frameLoaded && frameLoadError != null) {
                SessionRouteErrorScreen(
                    message = frameLoadError!!,
                    onRetry = { frameRetryKey++ },
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onBack = { navController.popBackStack() },
                )
            } else if (frameLoaded) when (flowStep) {
                "choose" -> FrameFlowCoordinatorScreen(
                    onColorPick = { saveFrame("color", onSaved = { flowStep = "color" }) },
                    onSeasonPick = { saveFrame("season", onSaved = { flowStep = "season" }) },
                    onOccasionPick = {
                        navController.navigate(
                            "${Routes.OCCASION_FRAME_PICK}/$frameTypeId/$sessionId/$selectedRaw/$layoutIdStr/${theme.id}",
                        )
                    },
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
                                seasonId = null, customDesignJson = null,
                                occasionThemeId = null, occasionDesignVersion = null)
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
                                customDesignJson = PendingCollageStore.serializeDesign(design),
                                occasionThemeId = null, occasionDesignVersion = null)
                        }
                    },
                )
                "occasion" -> OccasionFramePickRoute(
                    onBack = {
                        if (!occasionApplying) {
                            saveFrame("choose", onSaved = { flowStep = "choose" })
                        }
                    },
                    onDismiss = { if (!occasionApplying) navController.popBackStack() },
                    initialThemeId = initialOccasionThemeId,
                    saveError = frameSaveError,
                    applying = occasionApplying,
                    images = previewBitmaps,
                    cropTransforms = previewCropTransforms,
                    frameType = frameType,
                    frameStyle = frameStyle,
                    frameTheme = theme,
                    layoutVersion = layoutVersion,
                    filterId = previewFilterId,
                    captionTextPart = previewCaptionText,
                    captionDatePart = previewCaptionDate,
                    captionTextSizePt = previewTextSize,
                    captionDateSizePt = previewDateSize,
                    captionFontName = previewFontName,
                    captionColorRGB = previewColorRgb,
                    onCompleted = { occasion ->
                        if (occasionApplying) return@OccasionFramePickRoute
                        occasionApplying = true
                        saveFrame(
                            "edit",
                            onSaved = {
                                occasionApplying = false
                                navigateToEdit()
                            },
                            onFailed = { occasionApplying = false },
                        ) {
                            it.withOccasionSelection(occasion.id)
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
                                frameColorId = design.fillColorId, backgroundType = "solid",
                                occasionThemeId = null, occasionDesignVersion = null)
                        }
                    },
                    onDismiss = { design, failed ->
                        saveFrame("custom", onSaved = { navController.popBackStack() }, onFailed = failed) {
                            it.copy(customDesignJson = PendingCollageStore.serializeDesign(design),
                                frameColorId = design.fillColorId, backgroundType = "solid",
                                occasionThemeId = null, occasionDesignVersion = null)
                        }
                    },
                    initialDesign = initialDesign,
                    saveError = frameSaveError,
                    onDraftChanged = { design ->
                        saveFrame("custom") { it.copy(customDesignJson = PendingCollageStore.serializeDesign(design),
                            frameColorId = design.fillColorId, backgroundType = "solid",
                            occasionThemeId = null, occasionDesignVersion = null) }
                    },
                    onCompleted = { design, failed ->
                        saveFrame("edit", onSaved = { navigateToEdit() }, onFailed = failed) {
                            it.copy(frameColorId = design.fillColorId, backgroundType = "solid",
                                seasonId = null, customDesignJson = PendingCollageStore.serializeDesign(design),
                                occasionThemeId = null, occasionDesignVersion = null)
                        }
                    },
                )
            }
        }

        composable(
            route = "${Routes.OCCASION_FRAME_PICK}/{${Routes.Args.FRAME_TYPE}}/{${Routes.Args.SESSION_ID}}/{${Routes.Args.SELECTED_INDEXES}}/{${Routes.Args.LAYOUT_ID}}/{${Routes.Args.THEME_ID}}",
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
            val frameType = FrameType.fromId(frameTypeId)
            val frameLayoutId = remember(layoutId) {
                runCatching { FrameLayoutId.valueOf(layoutId) }.getOrElse { FrameLayoutId.FOUR_VERTICAL }
            }
            val frameStyle = remember(frameLayoutId) { FrameLayouts.byId(frameLayoutId) }
            val frameTheme = remember(frameType, themeId) {
                FrameCatalog.themes(frameType).firstOrNull { it.id == themeId }
                    ?: FrameCatalog.themes(frameType).first()
            }
            val context = LocalContext.current
            val sessions = remember(context) { SessionDocumentRepository(context) }
            val scope = rememberCoroutineScope()
            var saveError by remember { mutableStateOf<String?>(null) }
            var applying by remember { mutableStateOf(false) }
            var initialOccasionThemeId by remember { mutableStateOf<String?>(null) }
            var previewBitmaps by remember { mutableStateOf(emptyList<Bitmap>()) }
            var previewCropTransforms by remember { mutableStateOf(emptyList<PhotoCropTransform>()) }
            var previewLayoutVersion by remember { mutableIntStateOf(2) }
            var previewFilterId by remember { mutableStateOf(FilterId.ORIGINAL) }
            var previewCaptionText by remember { mutableStateOf<String?>(null) }
            var previewCaptionDate by remember { mutableStateOf<String?>(null) }
            var previewTextSize by remember { mutableStateOf(16f) }
            var previewDateSize by remember { mutableStateOf(16f) }
            var previewFontName by remember { mutableStateOf<String?>(null) }
            var previewColorRgb by remember { mutableStateOf<Long?>(null) }
            var documentLoaded by remember { mutableStateOf(false) }
            var documentNeedsRecovery by remember { mutableStateOf(false) }
            var documentLoadError by remember { mutableStateOf<String?>(null) }
            var documentRetryKey by remember { mutableIntStateOf(0) }
            LaunchedEffect(sessionId, documentRetryKey) {
                documentLoaded = false
                documentNeedsRecovery = false
                documentLoadError = null
                val document = try {
                    sessions.getById(sessionId)
                } catch (failure: Exception) {
                    documentLoadError = failure.message ?: "저장된 작업을 읽지 못했습니다."
                    documentLoaded = true
                    return@LaunchedEffect
                } ?: run {
                    documentNeedsRecovery = true
                    documentLoaded = true
                    return@LaunchedEffect
                }
                if (document.stage == SessionStage.NEEDS_RECOVERY) {
                    documentNeedsRecovery = true
                    documentLoaded = true
                    return@LaunchedEffect
                }
                initialOccasionThemeId = document.draft.occasionThemeId
                previewLayoutVersion = document.draft.layoutVersion
                val preview = try {
                    loadFramePreview(sessions, document, reqSize = 512)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Throwable) {
                    documentLoadError = failure.message ?: "사진 미리보기를 준비하지 못했습니다."
                    documentLoaded = true
                    return@LaunchedEffect
                }
                previewBitmaps = preview.images
                previewCropTransforms = preview.cropTransforms
                previewFilterId = preview.filterId
                previewCaptionText = preview.captionTextPart
                previewCaptionDate = preview.captionDatePart
                previewTextSize = preview.captionTextSizePt
                previewDateSize = preview.captionDateSizePt
                previewFontName = preview.captionFontName
                previewColorRgb = preview.captionColorRgb
                documentLoaded = true
            }

            fun dismissFrameFlow() {
                if (applying) return
                if (navController.popBackStack()) navController.popBackStack()
            }

            if (documentNeedsRecovery) {
                SessionRecoveryRequiredScreen(
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onBack = { navController.popBackStack() },
                )
            } else if (documentLoaded && documentLoadError != null) {
                SessionRouteErrorScreen(
                    message = documentLoadError!!,
                    onRetry = { documentRetryKey++ },
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onBack = { navController.popBackStack() },
                )
            } else if (documentLoaded) {
                OccasionFramePickRoute(
                    onBack = { if (!applying) navController.popBackStack() },
                    onDismiss = ::dismissFrameFlow,
                    initialThemeId = initialOccasionThemeId,
                    saveError = saveError,
                    applying = applying,
                    images = previewBitmaps,
                    cropTransforms = previewCropTransforms,
                    frameType = frameType,
                    frameStyle = frameStyle,
                    frameTheme = frameTheme,
                    layoutVersion = previewLayoutVersion,
                    filterId = previewFilterId,
                    captionTextPart = previewCaptionText,
                    captionDatePart = previewCaptionDate,
                    captionTextSizePt = previewTextSize,
                    captionDateSizePt = previewDateSize,
                    captionFontName = previewFontName,
                    captionColorRGB = previewColorRgb,
                    onCompleted = { occasion ->
                        if (applying) return@OccasionFramePickRoute
                        applying = true
                        scope.launch {
                            try {
                                val document = sessions.getById(sessionId)
                                    ?: error("편집할 작업을 찾지 못했습니다.")
                                sessions.update(sessionId, document.revision) { current ->
                                    current.copy(
                                        stage = SessionStage.EDIT,
                                        draft = current.draft.withOccasionSelection(occasion.id),
                                    )
                                }
                                saveError = null
                                navController.navigate(
                                    "${Routes.EDIT}/$frameTypeId/$sessionId/$selectedRaw/$layoutId/$themeId",
                                ) {
                                    // Replace the dedicated picker with Edit. Back from Edit then
                                    // reveals the single inline occasion step owned by FRAME_THEME_SELECT.
                                    popUpTo(requireNotNull(entry.destination.route)) { inclusive = true }
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                saveError = failure.message
                                    ?: "프레임을 저장하지 못했습니다. 다시 시도해 주세요."
                            } finally {
                                applying = false
                            }
                        }
                    },
                )
            } else {
                SessionRouteLoadingScreen()
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
            val context = LocalContext.current
            val sessions = remember(context) { SessionDocumentRepository(context) }
            var editGateLoaded by remember(sessionId) { mutableStateOf(false) }
            var editNeedsRecovery by remember(sessionId) { mutableStateOf(false) }
            var editLoadError by remember(sessionId) { mutableStateOf<String?>(null) }
            var editRetryKey by remember(sessionId) { mutableIntStateOf(0) }
            LaunchedEffect(sessionId, editRetryKey) {
                editGateLoaded = false
                editNeedsRecovery = false
                editLoadError = null
                try {
                    val document = sessions.getById(sessionId)
                    editNeedsRecovery = document == null || document.stage == SessionStage.NEEDS_RECOVERY
                } catch (failure: Exception) {
                    editLoadError = failure.message ?: "저장된 작업을 읽지 못했습니다."
                }
                editGateLoaded = true
            }
            when {
                !editGateLoaded -> SessionRouteLoadingScreen()
                editNeedsRecovery -> SessionRecoveryRequiredScreen(
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onBack = { navController.popBackStack() },
                )
                editLoadError != null -> SessionRouteErrorScreen(
                    message = editLoadError!!,
                    onRetry = { editRetryKey++ },
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onBack = { navController.popBackStack() },
                )
                else -> EditScreen(
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
            var detailOccasionTheme by remember { mutableStateOf<OccasionTheme?>(null) }
            var detailGateLoaded by remember(sessionId) { mutableStateOf(false) }
            var detailNeedsRecovery by remember(sessionId) { mutableStateOf(false) }
            var detailLoadError by remember(sessionId) { mutableStateOf<String?>(null) }
            var detailRetryKey by remember(sessionId) { mutableIntStateOf(0) }
            LaunchedEffect(sessionId, detailRetryKey) {
                detailGateLoaded = false
                detailNeedsRecovery = false
                detailLoadError = null
                detailDocument = null
                detailOccasionTheme = null
                photoIds = emptyList()
                photoPaths = emptyList()
                photoBitmaps = emptyList()

                val loadedBitmaps = mutableListOf<Bitmap>()
                var published = false
                try {
                    val document = sessions.getById(sessionId) ?: run {
                        detailNeedsRecovery = true
                        return@LaunchedEffect
                    }
                    if (document.stage == SessionStage.NEEDS_RECOVERY) {
                        detailNeedsRecovery = true
                        return@LaunchedEffect
                    }
                    val refs = document.draft.selectedPhotoIdsInOrder.mapNotNull { id ->
                        document.photos.firstOrNull { it.photoId == id }
                    }
                    if (refs.isEmpty() || refs.size != document.draft.selectedPhotoIdsInOrder.size) {
                        detailNeedsRecovery = true
                        return@LaunchedEffect
                    }
                    val paths = refs.map { sessions.resolvePhotoPath(it).absolutePath }
                    val occasionTheme = document.draft.takeIf {
                        it.backgroundType == "occasion" &&
                            it.occasionDesignVersion == OccasionCatalogContract.SESSION_DESIGN_VERSION
                    }?.occasionThemeId?.let { occasionId ->
                        withContext(Dispatchers.IO) {
                            OccasionCatalogLoader.load(context.applicationContext).theme(occasionId)
                        }
                    }
                    paths.forEach { path ->
                        val bitmap = BitmapDecoding.decodeSampled(path, 1024)
                            ?: error("사진 미리보기를 불러오지 못했습니다.")
                        loadedBitmaps += bitmap
                    }
                    detailDocument = document
                    detailOccasionTheme = occasionTheme
                    photoIds = refs.map { it.photoId }
                    photoPaths = paths
                    photoBitmaps = loadedBitmaps.toList()
                    published = true
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Throwable) {
                    detailLoadError = when (failure) {
                        is OutOfMemoryError -> "사진을 불러올 메모리가 부족합니다. 다른 앱을 닫은 뒤 다시 시도해 주세요."
                        else -> failure.message ?: "사진과 프레임을 불러오지 못했습니다."
                    }
                } finally {
                    if (!published) {
                        loadedBitmaps.forEach { bitmap ->
                            if (!bitmap.isRecycled) bitmap.recycle()
                        }
                    }
                    detailGateLoaded = true
                }
            }

            if (detailNeedsRecovery) {
                SessionRecoveryRequiredScreen(
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onBack = { navController.popBackStack() },
                )
            } else if (!detailGateLoaded) {
                SessionRouteLoadingScreen()
            } else if (detailLoadError != null) {
                SessionRouteErrorScreen(
                    message = detailLoadError!!,
                    onRetry = { detailRetryKey++ },
                    onOpenGallery = { navController.navigate(Routes.GALLERY) },
                    onBack = { navController.popBackStack() },
                )
            } else if (photoBitmaps.size == photoIds.size && photoBitmaps.isNotEmpty() && detailDocument != null) {
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
                    occasionTheme = detailOccasionTheme,
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
    val frameType = FrameType.fromId(document.frameTypeId)
    val id = document.sessionId
    if (document.stage == SessionStage.IMPORT && document.inputSource == InputSource.ALBUM) {
        return "${Routes.PHOTO_IMPORT}/${frameType.id}/$id"
    }
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

internal fun SessionDraft.withOccasionSelection(themeId: String): SessionDraft = copy(
    frameStep = "occasion",
    backgroundType = "occasion",
    occasionThemeId = themeId,
    occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
    seasonId = null,
    customDesignJson = null,
)

private data class FramePreviewData(
    val images: List<Bitmap> = emptyList(),
    val cropTransforms: List<PhotoCropTransform> = emptyList(),
    val filterId: FilterId = FilterId.ORIGINAL,
    val captionTextPart: String? = null,
    val captionDatePart: String? = null,
    val captionTextSizePt: Float = 16f,
    val captionDateSizePt: Float = 16f,
    val captionFontName: String? = null,
    val captionColorRgb: Long? = null,
)

/** Builds the frame-picker proxy from the same persisted photo order and edit values as export. */
private suspend fun loadFramePreview(
    sessions: SessionDocumentRepository,
    document: SessionDocument,
    reqSize: Int,
): FramePreviewData = withContext(Dispatchers.IO) {
    val refs = document.draft.selectedPhotoIdsInOrder.map { id ->
        document.photos.firstOrNull { it.photoId == id }
            ?: error("선택한 사진 기록이 없어 작업 복구가 필요합니다.")
    }
    require(refs.isNotEmpty()) { "선택한 사진이 없어 작업 복구가 필요합니다." }
    val adjustedImages = mutableListOf<Bitmap>()
    val selectedFilter = runCatching { FilterId.valueOf(document.draft.filterId) }
        .getOrDefault(FilterId.ORIGINAL)
    try {
        refs.forEach { ref ->
            val adjustment = document.draft.adjustmentsByPhotoId[ref.photoId] ?: PhotoAdjustments()
            val decoded = decodeAdjustedFramePreview(
                path = sessions.resolvePhotoPath(ref).absolutePath,
                adjustment = adjustment,
                filterId = selectedFilter,
                reqSize = reqSize,
            ) ?: error("미리보기 사진을 불러오지 못했습니다.")
            adjustedImages += decoded
        }
    } catch (cancelled: CancellationException) {
        adjustedImages.forEach { if (!it.isRecycled) it.recycle() }
        throw cancelled
    } catch (failure: Throwable) {
        adjustedImages.forEach { if (!it.isRecycled) it.recycle() }
        throw failure
    }

    val draft = document.draft
    FramePreviewData(
        images = adjustedImages,
        cropTransforms = refs.map { ref ->
            val adjustment = draft.adjustmentsByPhotoId[ref.photoId] ?: PhotoAdjustments()
            PhotoCropTransform(
                crop = adjustment.crop,
                quarterTurnsClockwise = ((adjustment.rotationDegrees / 90) % 4 + 4) % 4,
                flipHorizontal = adjustment.flipHorizontal,
            )
        },
        // Each proxy image already contains the shared filter → per-photo adjustment pipeline.
        filterId = FilterId.ORIGINAL,
        captionTextPart = draft.caption.trim().takeIf { it.isNotEmpty() },
        captionDatePart = draft.dateText.takeIf { draft.showDate },
        captionTextSizePt = draft.textFontSize,
        captionDateSizePt = draft.dateFontSize,
        captionFontName = draft.captionFontName,
        captionColorRgb = draft.captionColorRgb,
    )
}

private fun decodeAdjustedFramePreview(
    path: String,
    adjustment: PhotoAdjustments,
    filterId: FilterId,
    reqSize: Int,
): Bitmap? {
    val decoded = BitmapDecoding.decodeSampled(path, reqSize) ?: return null
    return PhotoEditPipeline.applyOwned(
        source = decoded,
        quarterTurnsClockwise = adjustment.rotationDegrees / 90,
        flipHorizontal = adjustment.flipHorizontal,
        filterId = filterId,
        brightness = adjustment.brightness,
        contrast = adjustment.contrast,
        saturation = adjustment.saturation,
    )
}
