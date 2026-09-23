package com.pocket4cut.presentation.edit

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.domain.model.PhotoAdjustments
import com.pocket4cut.frame.CustomFrameDesign
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameColor
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.PhotoCropTransform
import com.pocket4cut.frame.PhotoEditPipeline
import com.pocket4cut.frame.occasion.OccasionCatalogContract
import com.pocket4cut.frame.occasion.OccasionCatalogLoader
import com.pocket4cut.frame.occasion.OccasionTheme
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.presentation.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EditUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedFilter: FilterId = FilterId.ORIGINAL,
    val customText: String = "",
    val showDate: Boolean = AppSettings.showDateByDefault,
    val selectedFrameColor: FrameColor = FrameColors.all.first(),
    val dateString: String = "",
    val order: List<Int> = emptyList(),
    val selectedSwapIndex: Int? = null,
    val orderedImages: List<Bitmap> = emptyList(),
    val filterChipThumbnails: Map<FilterId, Bitmap> = emptyMap(),
    val textFontSize: Float = 16f,
    val dateFontSize: Float = 16f,
    val captionFontName: String? = null,
    val captionColorRGB: Long? = null,
    val frameBackgroundType: String = "solid",
    val seasonId: String? = null,
    val customFrameDesign: CustomFrameDesign? = null,
    val allowsColorEditInEditor: Boolean = true,
    val layoutVersion: Int = 2,
    val cropTransforms: List<PhotoCropTransform> = emptyList(),
    val occasionTheme: OccasionTheme? = null,
)

private data class LoadedEditData(
    val document: com.pocket4cut.domain.model.SessionDocument,
    val photoIds: List<String>,
    val sourceImages: List<Bitmap>,
    val previewImages: List<Bitmap>,
    val adjustments: List<PhotoAdjustments>,
    val cropTransforms: List<PhotoCropTransform>,
    val occasionTheme: OccasionTheme?,
)

class EditViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = FileImageStorage(app.applicationContext)
    private val sessions = SessionDocumentRepository(app.applicationContext)
    private var persistJob: Job? = null
    private var navigationInFlight = false
    private val persistMutex = Mutex()
    private var basePhotoIds: List<String> = emptyList()
    private var baseAdjustments: List<PhotoAdjustments> = emptyList()
    private var baseCropTransforms: List<PhotoCropTransform> = emptyList()
    private var previewImagesByBaseIndex: List<Bitmap> = emptyList()
    private var filterPreviewJob: Job? = null
    private var filterPreviewGeneration = 0L
    /** Filter represented by [previewImagesByBaseIndex], and therefore safe to persist/export. */
    private var publishedPreviewFilter: FilterId = FilterId.ORIGINAL
    private var loadGeneration = 0L

    internal var beforeFilterPreviewPublishForTest: (suspend () -> Unit)? = null
    internal var beforeFilterChipPublishForTest: (suspend () -> Unit)? = null

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState

    // These bitmaps are also published in uiState. Compose may retain a previous state while it
    // completes a draw, so their lifetime is managed by reachability instead of manual recycle().
    private var originalImages: List<Bitmap> = emptyList()
    private var lastSessionId: String = ""
    private var lastFrameType: FrameType? = null
    private var lastFrameLayoutId: FrameLayoutId? = null
    private var lastSelectedIndexes: List<Int> = emptyList()

    fun init(
        frameType: FrameType,
        sessionId: String,
        selectedIndexes: List<Int>,
        frameLayoutId: FrameLayoutId,
    ) {
        val generation = ++loadGeneration
        filterPreviewGeneration += 1
        filterPreviewJob?.cancel()
        lastFrameType = frameType
        lastFrameLayoutId = frameLayoutId
        lastSelectedIndexes = selectedIndexes
        lastSessionId = sessionId

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                val document = sessions.getById(sessionId) ?: error("편집할 작업을 찾지 못했습니다.")
                val ids = document.draft.selectedPhotoIdsInOrder
                check(ids.size == frameType.selectCount) { "선택한 사진 수가 맞지 않습니다." }
                val paths = ids.map { id ->
                    val photo = document.photos.firstOrNull { it.photoId == id } ?: error("선택한 사진이 없습니다.")
                    sessions.resolvePhotoPath(photo).also { check(it.isFile) { "사진 파일이 없습니다." } }.absolutePath
                }
                val occasionTheme = if (document.draft.backgroundType == "occasion") {
                    check(document.draft.occasionDesignVersion == OccasionCatalogContract.SESSION_DESIGN_VERSION) {
                        "지원하지 않는 기념일 프레임 버전입니다."
                    }
                    val occasionId = document.draft.occasionThemeId
                        ?: error("기념일 프레임 ID가 없습니다.")
                    withContext(Dispatchers.IO) {
                        OccasionCatalogLoader.load(getApplication()).theme(occasionId)
                            ?: error("기념일 프레임을 찾을 수 없습니다: $occasionId")
                    }
                } else {
                    null
                }
                val adjustments = ids.map { id -> document.draft.adjustmentsByPhotoId[id] ?: PhotoAdjustments() }
                val selectedFilter = runCatching { FilterId.valueOf(document.draft.filterId) }
                    .getOrDefault(FilterId.ORIGINAL)
                val sourceImages = withContext(Dispatchers.IO) {
                    val decoded = mutableListOf<Bitmap>()
                    try {
                        paths.forEach { path ->
                            decoded += BitmapDecoding.decodeSampled(path, reqSize = 720)
                                ?: error("사진을 열 수 없습니다.")
                        }
                        decoded
                    } catch (cause: Throwable) {
                        decoded.forEach(Bitmap::recycle)
                        throw cause
                    }
                }
                val previewImages = try {
                    withContext(Dispatchers.Default) {
                        renderPreviewImages(sourceImages, adjustments, selectedFilter)
                    }
                } catch (cause: Throwable) {
                    sourceImages.forEach(Bitmap::recycle)
                    throw cause
                }
                LoadedEditData(
                    document = document,
                    photoIds = ids,
                    sourceImages = sourceImages,
                    previewImages = previewImages,
                    adjustments = adjustments,
                    cropTransforms = adjustments.map { adjustment ->
                        PhotoCropTransform(
                            crop = adjustment.crop,
                            quarterTurnsClockwise = ((adjustment.rotationDegrees / 90) % 4 + 4) % 4,
                            flipHorizontal = adjustment.flipHorizontal,
                        )
                    },
                    occasionTheme = occasionTheme,
                )
            }.onSuccess { loaded ->
                if (generation != loadGeneration) {
                    loaded.sourceImages.forEach(Bitmap::recycle)
                    loaded.previewImages.forEach(Bitmap::recycle)
                    return@onSuccess
                }
                val document = loaded.document
                val ids = loaded.photoIds
                val sourceImages = loaded.sourceImages
                val previewImages = loaded.previewImages
                basePhotoIds = ids
                baseAdjustments = loaded.adjustments
                baseCropTransforms = loaded.cropTransforms
                originalImages = sourceImages
                previewImagesByBaseIndex = previewImages
                val order = previewImages.indices.toList()
                val draft = document.draft
                val loadedFilter = runCatching { FilterId.valueOf(draft.filterId) }
                    .getOrDefault(FilterId.ORIGINAL)
                publishedPreviewFilter = loadedFilter
                val design = draft.customDesignJson?.let { PendingCollageStore.deserializeDesign(it) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        order = order,
                        orderedImages = previewImages,
                        selectedFilter = loadedFilter,
                        customText = draft.caption,
                        showDate = draft.showDate,
                        dateString = draft.dateText,
                        selectedFrameColor = FrameColors.byId(draft.frameColorId),
                        frameBackgroundType = draft.backgroundType,
                        seasonId = draft.seasonId,
                        customFrameDesign = design,
                        allowsColorEditInEditor = draft.backgroundType != "season" &&
                            draft.backgroundType != "occasion" && design?.sourceSeason == null,
                        layoutVersion = draft.layoutVersion,
                        textFontSize = draft.textFontSize,
                        dateFontSize = draft.dateFontSize,
                        captionFontName = draft.captionFontName,
                        captionColorRGB = draft.captionColorRgb,
                        cropTransforms = loaded.cropTransforms,
                        occasionTheme = loaded.occasionTheme,
                    )
                }
                buildFilterChipThumbnails(generation)
            }.onFailure { t ->
                if (generation == loadGeneration) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = t.message) }
                }
            }
        }
    }

    fun setFilter(filter: FilterId) {
        if (_uiState.value.isLoading || navigationInFlight || _uiState.value.selectedFilter == filter) return
        _uiState.update { it.copy(selectedFilter = filter, errorMessage = null) }
        rebuildPreviewForFilter(filter)
    }

    fun setFrameColor(color: FrameColor) {
        _uiState.update {
            it.copy(
                selectedFrameColor = color,
                customFrameDesign = it.customFrameDesign?.copy(fillColorId = color.id, fillHex = null),
            )
        }
        schedulePersist()
    }

    fun setText(text: String) {
        _uiState.update { it.copy(customText = text) }
        schedulePersist(300)
    }

    fun toggleDate() {
        _uiState.update { it.copy(showDate = !it.showDate) }
        schedulePersist()
    }

    fun setShowDate(show: Boolean) {
        _uiState.update { it.copy(showDate = show) }
        schedulePersist()
    }

    fun setTextFontSize(size: Float) {
        val v = size.coerceIn(1f, 30f)
        _uiState.update { it.copy(textFontSize = v) }
        schedulePersist(300)
    }

    fun setDateFontSize(size: Float) {
        val v = size.coerceIn(1f, 30f)
        _uiState.update { it.copy(dateFontSize = v) }
        schedulePersist(300)
    }

    fun setCaptionFontName(name: String?) {
        _uiState.update { it.copy(captionFontName = name) }
        schedulePersist()
    }

    fun setCaptionColorRGB(rgb: Long?) {
        _uiState.update { it.copy(captionColorRGB = rgb) }
        schedulePersist()
    }

    fun tapOrderCell(index: Int) {
        val state = _uiState.value
        val current = state.selectedSwapIndex
        when {
            current == null -> _uiState.update { it.copy(selectedSwapIndex = index) }
            current == index -> _uiState.update { it.copy(selectedSwapIndex = null) }
            else -> {
                val newOrder = state.order.toMutableList()
                val temp = newOrder[current]
                newOrder[current] = newOrder[index]
                newOrder[index] = temp
                _uiState.update { it.copy(order = newOrder, selectedSwapIndex = null) }
                rebuildOrderedAndFilteredImages()
                schedulePersist()
            }
        }
    }

    /** 표시 순서 기준으로 [fromIndex] 칸을 [toIndex] 위치로 옮김 (iOS moveImage 대응). */
    fun moveOrderSlot(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val orderList = _uiState.value.order
        if (fromIndex !in orderList.indices || toIndex !in orderList.indices) return
        val newOrder = orderList.toMutableList()
        val elem = newOrder.removeAt(fromIndex)
        newOrder.add(toIndex, elem)
        _uiState.update { it.copy(order = newOrder, selectedSwapIndex = null) }
        rebuildOrderedAndFilteredImages()
        schedulePersist()
    }

    private fun schedulePersist(waitMillis: Long = 120L) {
        val snapshot = _uiState.value
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(waitMillis)
            try {
                persistSnapshot(snapshot, SessionStage.EDIT)
            } catch (cause: kotlinx.coroutines.CancellationException) {
                throw cause
            } catch (cause: Exception) {
                _uiState.update { it.copy(errorMessage = cause.message ?: "편집 내용을 저장하지 못했습니다.") }
            }
        }
    }

    private suspend fun persistSnapshot(snapshot: EditUiState, stage: SessionStage) = persistMutex.withLock {
        val id = lastSessionId.takeIf { it.isNotBlank() } ?: error("작업 ID가 없습니다.")
        val orderedIds = snapshot.order.map { basePhotoIds[it] }
        val document = sessions.getById(id) ?: error("편집할 작업을 찾지 못했습니다.")
        sessions.update(id, document.revision) { current ->
            current.copy(
                stage = stage,
                draft = current.draft.copy(
                    selectedPhotoIdsInOrder = orderedIds,
                    filterId = snapshot.selectedFilter.name,
                    frameColorId = snapshot.selectedFrameColor.id,
                    backgroundType = snapshot.frameBackgroundType,
                    caption = snapshot.customText,
                    showDate = snapshot.showDate,
                    textFontSize = snapshot.textFontSize,
                    dateFontSize = snapshot.dateFontSize,
                    captionFontName = snapshot.captionFontName,
                    captionColorRgb = snapshot.captionColorRGB,
                    seasonId = snapshot.seasonId,
                    customDesignJson = snapshot.customFrameDesign?.let { PendingCollageStore.serializeDesign(it) },
                ),
            )
        }
    }

    fun leave(onSaved: () -> Unit) {
        if (navigationInFlight) return
        navigationInFlight = true
        viewModelScope.launch {
            try {
                filterPreviewJob?.join()
                persistJob?.cancelAndJoin()
                persistSnapshot(_uiState.value, SessionStage.EDIT)
                onSaved()
            } catch (cause: kotlinx.coroutines.CancellationException) {
                throw cause
            } catch (cause: Exception) {
                _uiState.update { it.copy(errorMessage = cause.message ?: "편집 내용을 저장하지 못했습니다.") }
            } finally {
                navigationInFlight = false
            }
        }
    }

    fun persistPendingForDetailEdit(sessionId: String, onSaved: () -> Unit) {
        if (navigationInFlight) return
        navigationInFlight = true
        viewModelScope.launch {
            try {
                filterPreviewJob?.join()
                persistJob?.cancelAndJoin()
                persistSnapshot(_uiState.value, SessionStage.DETAIL)
                onSaved()
            } catch (cause: kotlinx.coroutines.CancellationException) {
                throw cause
            } catch (cause: Exception) {
                _uiState.update { it.copy(errorMessage = cause.message ?: "편집 내용을 저장하지 못했습니다.") }
            } finally {
                navigationInFlight = false
            }
        }
    }

    private fun buildFilterChipThumbnails(loadToken: Long) {
        val first = originalImages.firstOrNull() ?: return
        val adjustment = baseAdjustments.firstOrNull() ?: PhotoAdjustments()
        viewModelScope.launch {
            val thumbs = try {
                withContext(Dispatchers.Default) {
                    val smallThumb = scaleBitmap(first, 128)
                    val created = linkedMapOf<FilterId, Bitmap>()
                    try {
                        FilterId.entries.forEach { filterId ->
                            currentCoroutineContext().ensureActive()
                            created[filterId] = PhotoEditPipeline.applyOwned(
                                source = smallThumb.copy(Bitmap.Config.ARGB_8888, false),
                                quarterTurnsClockwise = adjustment.rotationDegrees / 90,
                                flipHorizontal = adjustment.flipHorizontal,
                                filterId = filterId,
                                brightness = adjustment.brightness,
                                contrast = adjustment.contrast,
                                saturation = adjustment.saturation,
                            )
                        }
                        created.toMap()
                    } catch (cause: Throwable) {
                        created.values.forEach(Bitmap::recycle)
                        throw cause
                    } finally {
                        if (smallThumb !== first) smallThumb.recycle()
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (cause: Throwable) {
                if (loadToken == loadGeneration) {
                    _uiState.update { it.copy(errorMessage = cause.message ?: "필터 썸네일을 만들지 못했습니다.") }
                }
                return@launch
            }
            var published = false
            try {
                beforeFilterChipPublishForTest?.invoke()
                currentCoroutineContext().ensureActive()
                if (loadToken != loadGeneration) return@launch
                _uiState.update { it.copy(filterChipThumbnails = thumbs) }
                published = true
            } finally {
                if (!published) thumbs.values.forEach(Bitmap::recycle)
            }
        }
    }

    private fun rebuildOrderedAndFilteredImages() {
        val order = _uiState.value.order
        val ordered = order.mapNotNull { previewImagesByBaseIndex.getOrNull(it) }
        val crops = order.mapNotNull { baseCropTransforms.getOrNull(it) }
        _uiState.update { it.copy(orderedImages = ordered, cropTransforms = crops) }
    }

    private fun rebuildPreviewForFilter(filter: FilterId) {
        val generation = ++filterPreviewGeneration
        filterPreviewJob?.cancel()
        val sources = originalImages
        val adjustments = baseAdjustments
        if (sources.size != adjustments.size || sources.isEmpty()) {
            _uiState.update {
                it.copy(
                    selectedFilter = publishedPreviewFilter,
                    errorMessage = "필터 미리보기를 만들 사진이 없습니다.",
                )
            }
            return
        }
        filterPreviewJob = viewModelScope.launch {
            val previews = try {
                withContext(Dispatchers.Default) {
                    renderPreviewImages(sources, adjustments, filter)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (cause: Throwable) {
                if (generation == filterPreviewGeneration && filter == _uiState.value.selectedFilter) {
                    _uiState.update {
                        it.copy(
                            selectedFilter = publishedPreviewFilter,
                            errorMessage = cause.message ?: "필터 미리보기를 만들지 못했습니다.",
                        )
                    }
                }
                return@launch
            }
            var published = false
            try {
                beforeFilterPreviewPublishForTest?.invoke()
                currentCoroutineContext().ensureActive()
                if (generation != filterPreviewGeneration || filter != _uiState.value.selectedFilter) {
                    return@launch
                }
                previewImagesByBaseIndex = previews
                publishedPreviewFilter = filter
                rebuildOrderedAndFilteredImages()
                _uiState.update { it.copy(errorMessage = null) }
                published = true
                schedulePersist()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (cause: Throwable) {
                if (generation == filterPreviewGeneration && filter == _uiState.value.selectedFilter) {
                    _uiState.update {
                        it.copy(
                            selectedFilter = publishedPreviewFilter,
                            errorMessage = cause.message ?: "필터 미리보기를 만들지 못했습니다.",
                        )
                    }
                }
            } finally {
                if (!published) previews.forEach(Bitmap::recycle)
            }
        }
    }

    private suspend fun renderPreviewImages(
        sources: List<Bitmap>,
        adjustments: List<PhotoAdjustments>,
        filter: FilterId,
    ): List<Bitmap> {
        val rendered = mutableListOf<Bitmap>()
        val coroutineContext = currentCoroutineContext()
        try {
            sources.forEachIndexed { index, source ->
                coroutineContext.ensureActive()
                val adjustment = adjustments[index]
                rendered += PhotoEditPipeline.applyOwned(
                    source = source.copy(Bitmap.Config.ARGB_8888, false),
                    quarterTurnsClockwise = adjustment.rotationDegrees / 90,
                    flipHorizontal = adjustment.flipHorizontal,
                    filterId = filter,
                    brightness = adjustment.brightness,
                    contrast = adjustment.contrast,
                    saturation = adjustment.saturation,
                    checkCancelled = { coroutineContext.ensureActive() },
                )
            }
            return rendered
        } catch (cause: Throwable) {
            rendered.forEach(Bitmap::recycle)
            throw cause
        }
    }

    companion object {
        private fun scaleBitmap(source: Bitmap, maxDim: Int): Bitmap {
            val scale = maxDim.toFloat() / maxOf(source.width, source.height)
            if (scale >= 1f) return source
            val w = (source.width * scale).toInt().coerceAtLeast(1)
            val h = (source.height * scale).toInt().coerceAtLeast(1)
            return Bitmap.createScaledBitmap(source, w, h, true)
        }
    }
}
