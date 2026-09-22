package com.pocket4cut.presentation.edit

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.frame.CustomFrameDesign
import com.pocket4cut.frame.FilterDefs
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameColor
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.presentation.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
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
)

class EditViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = FileImageStorage(app.applicationContext)
    private val sessions = SessionDocumentRepository(app.applicationContext)
    private var persistJob: Job? = null
    private val persistMutex = Mutex()
    private var basePhotoIds: List<String> = emptyList()

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
        if (lastSessionId == sessionId && originalImages.isNotEmpty()) return
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
                val bitmaps = withContext(Dispatchers.IO) {
                    paths.map { BitmapDecoding.decodeSampled(it, reqSize = 720) ?: error("사진을 열 수 없습니다.") }
                }
                Triple(document, ids, bitmaps)
            }.onSuccess { (document, ids, bitmaps) ->
                basePhotoIds = ids
                originalImages = bitmaps
                val order = bitmaps.indices.toList()
                val draft = document.draft
                val design = draft.customDesignJson?.let { PendingCollageStore.deserializeDesign(it) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        order = order,
                        orderedImages = bitmaps,
                        selectedFilter = runCatching { FilterId.valueOf(draft.filterId) }.getOrDefault(FilterId.ORIGINAL),
                        customText = draft.caption,
                        showDate = draft.showDate,
                        dateString = draft.dateText,
                        selectedFrameColor = FrameColors.byId(draft.frameColorId),
                        frameBackgroundType = draft.backgroundType,
                        seasonId = draft.seasonId,
                        customFrameDesign = design,
                        allowsColorEditInEditor = draft.backgroundType != "season" && design?.sourceSeason == null,
                        textFontSize = draft.textFontSize,
                        dateFontSize = draft.dateFontSize,
                        captionFontName = draft.captionFontName,
                        captionColorRGB = draft.captionColorRgb,
                    )
                }
                buildFilterChipThumbnails()
            }.onFailure { t ->
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message) }
            }
        }
    }

    fun setFilter(filter: FilterId) {
        if (_uiState.value.selectedFilter == filter) return
        _uiState.update { it.copy(selectedFilter = filter) }
        schedulePersist()
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
        viewModelScope.launch {
            try {
                persistJob?.cancelAndJoin()
                persistSnapshot(_uiState.value, SessionStage.EDIT)
                onSaved()
            } catch (cause: Exception) {
                _uiState.update { it.copy(errorMessage = cause.message ?: "편집 내용을 저장하지 못했습니다.") }
            }
        }
    }

    fun persistPendingForDetailEdit(sessionId: String, onSaved: () -> Unit) {
        val s = _uiState.value
        viewModelScope.launch {
            try {
                persistJob?.cancelAndJoin()
                persistSnapshot(s, SessionStage.DETAIL)
                onSaved()
            } catch (cause: Exception) {
                _uiState.update { it.copy(errorMessage = cause.message ?: "편집 내용을 저장하지 못했습니다.") }
            }
        }
    }

    private fun buildFilterChipThumbnails() {
        val first = originalImages.firstOrNull() ?: return
        viewModelScope.launch {
            val thumbs = withContext(Dispatchers.Default) {
                val smallThumb = scaleBitmap(first, 128)
                FilterId.entries.associateWith { filterId ->
                    applyFilterToBitmap(smallThumb, filterId)
                }
            }
            _uiState.update { it.copy(filterChipThumbnails = thumbs) }
        }
    }

    private fun rebuildOrderedAndFilteredImages() {
        val order = _uiState.value.order
        val ordered = order.mapNotNull { originalImages.getOrNull(it) }
        _uiState.update { it.copy(orderedImages = ordered) }
    }

    companion object {
        fun applyFilterToBitmap(source: Bitmap, filterId: FilterId): Bitmap {
            if (filterId == FilterId.ORIGINAL) return source
            val cf = FilterDefs.colorFilter(filterId) ?: return source
            val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(result)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    colorFilter = cf
                }
                canvas.drawBitmap(source, 0f, 0f, paint)
                return result
            } catch (cause: Throwable) {
                result.recycle()
                throw cause
            }
        }

        private fun scaleBitmap(source: Bitmap, maxDim: Int): Bitmap {
            val scale = maxDim.toFloat() / maxOf(source.width, source.height)
            if (scale >= 1f) return source
            val w = (source.width * scale).toInt().coerceAtLeast(1)
            val h = (source.height * scale).toInt().coerceAtLeast(1)
            return Bitmap.createScaledBitmap(source, w, h, true)
        }
    }
}
