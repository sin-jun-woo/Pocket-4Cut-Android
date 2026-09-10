package com.pocket4cut.presentation.edit

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.data.storage.FileImageStorage
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val filteredPreviewImages: List<Bitmap> = emptyList(),
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

        val date = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

        val frameSel = PendingCollageStore.readFrameSelection(getApplication(), sessionId)
        val bgType = frameSel?.type ?: "solid"
        val seasonIdVal = frameSel?.seasonId
        val designJson = frameSel?.customDesignJson
        val design = designJson?.let { PendingCollageStore.deserializeDesign(it) }
        val frameColor = frameSel?.frameColorId?.let { FrameColors.byId(it) } ?: FrameColors.all.first()
        val allowsColor = when (bgType) {
            "solid" -> true
            "custom" -> design?.sourceSeason == null
            else -> false
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                dateString = date,
                frameBackgroundType = bgType,
                seasonId = seasonIdVal,
                customFrameDesign = design,
                selectedFrameColor = frameColor,
                allowsColorEditInEditor = allowsColor,
            )
        }

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val allPaths = storage.getCapturePaths(sessionId)
                    val picked = selectedIndexes.mapNotNull { idx -> allPaths.getOrNull(idx) }
                    picked.mapNotNull { BitmapDecoding.decodeSampled(it, reqSize = 720) }
                }
            }.onSuccess { bitmaps ->
                originalImages = bitmaps
                val order = bitmaps.indices.toList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        order = order,
                        orderedImages = bitmaps,
                        filteredPreviewImages = bitmaps,
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
        rebuildFilteredImages()
    }

    fun setFrameColor(color: FrameColor) {
        _uiState.update { it.copy(selectedFrameColor = color) }
    }

    fun setText(text: String) {
        _uiState.update { it.copy(customText = text) }
    }

    fun toggleDate() {
        _uiState.update { it.copy(showDate = !it.showDate) }
    }

    fun setShowDate(show: Boolean) {
        _uiState.update { it.copy(showDate = show) }
    }

    fun setTextFontSize(size: Float) {
        val v = size.coerceIn(1f, 30f)
        _uiState.update { it.copy(textFontSize = v) }
    }

    fun setDateFontSize(size: Float) {
        val v = size.coerceIn(1f, 30f)
        _uiState.update { it.copy(dateFontSize = v) }
    }

    fun setCaptionFontName(name: String?) {
        _uiState.update { it.copy(captionFontName = name) }
    }

    fun setCaptionColorRGB(rgb: Long?) {
        _uiState.update { it.copy(captionColorRGB = rgb) }
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
    }

    fun persistPendingForDetailEdit(sessionId: String) {
        val s = _uiState.value
        PendingCollageStore.write(
            getApplication(), sessionId,
            PendingCollageParams(
                filterId = s.selectedFilter,
                frameColorId = s.selectedFrameColor.id,
                text = s.customText,
                showDate = s.showDate,
                order = s.order,
                textFontSize = s.textFontSize,
                dateFontSize = s.dateFontSize,
                captionFontName = s.captionFontName,
                captionColorRGB = s.captionColorRGB,
                frameBackgroundType = s.frameBackgroundType,
                seasonId = s.seasonId,
                customDesignJson = s.customFrameDesign?.let { PendingCollageStore.serializeDesign(it) },
            ),
        )
    }

    suspend fun renderFinalAndSave(sessionId: String): String {
        val s = _uiState.value
        val params = PendingCollageParams(
            filterId = s.selectedFilter,
            frameColorId = s.selectedFrameColor.id,
            text = s.customText,
            showDate = s.showDate,
            order = s.order,
            textFontSize = s.textFontSize,
            dateFontSize = s.dateFontSize,
            captionFontName = s.captionFontName,
            captionColorRGB = s.captionColorRGB,
        )
        return CollageFinalize.finalize(
            getApplication(), sessionId,
            lastFrameType ?: error("frameType missing"),
            lastFrameLayoutId ?: error("layoutId missing"),
            lastSelectedIndexes,
            params,
        )
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
        val filter = _uiState.value.selectedFilter
        val filtered = if (filter == FilterId.ORIGINAL) ordered
        else ordered.map { applyFilterToBitmap(it, filter) }
        _uiState.update { it.copy(orderedImages = ordered, filteredPreviewImages = filtered) }
    }

    private fun rebuildFilteredImages() {
        val ordered = _uiState.value.orderedImages
        if (ordered.isEmpty()) return
        viewModelScope.launch {
            val filter = _uiState.value.selectedFilter
            val filtered = withContext(Dispatchers.Default) {
                if (filter == FilterId.ORIGINAL) ordered
                else ordered.map { applyFilterToBitmap(it, filter) }
            }
            _uiState.update { it.copy(filteredPreviewImages = filtered) }
        }
    }

    companion object {
        fun applyFilterToBitmap(source: Bitmap, filterId: FilterId): Bitmap {
            if (filterId == FilterId.ORIGINAL) return source
            val cf = FilterDefs.colorFilter(filterId) ?: return source
            val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = cf
            }
            canvas.drawBitmap(source, 0f, 0f, paint)
            return result
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
