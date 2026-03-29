package com.pocket4cut.presentation.edit

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.frame.*
import com.pocket4cut.presentation.navigation.FrameType
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
    val imagePaths: List<String> = emptyList(),
    val order: List<Int> = emptyList(),
    val selectedFilter: FilterId = FilterId.ORIGINAL,
    val selectedFrameColor: FrameColor = FrameColors.all.first(),
    val text: String = "",
    val showDate: Boolean = false,
    val dateString: String = "",
    val preview: Bitmap? = null,
)

class EditViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = FileImageStorage(app.applicationContext)

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState

    private var lastFrameType: FrameType? = null
    private var lastFrameLayoutId: FrameLayoutId? = null
    private var lastSelectedIndexes: List<Int> = emptyList()

    fun init(
        frameType: FrameType,
        sessionId: String,
        selectedIndexes: List<Int>,
        frameLayoutId: FrameLayoutId,
    ) {
        lastFrameType = frameType
        lastFrameLayoutId = frameLayoutId
        lastSelectedIndexes = selectedIndexes
        val date = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(isLoading = true, errorMessage = null, preview = null, dateString = date) }
        viewModelScope.launch {
            runCatching { storage.getCapturePaths(sessionId) }
                .onSuccess { allPaths ->
                    val picked = selectedIndexes.mapNotNull { idx -> allPaths.getOrNull(idx) }
                    _uiState.update { it.copy(isLoading = false, imagePaths = picked, order = picked.indices.toList()) }
                    renderPreview()
                }
                .onFailure { t ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = t.message) }
                }
        }
    }

    fun setFilter(filter: FilterId) {
        _uiState.update { it.copy(selectedFilter = filter) }
        renderPreview()
    }

    fun setFrameColor(color: FrameColor) {
        _uiState.update { it.copy(selectedFrameColor = color) }
        renderPreview()
    }

    fun setText(text: String) {
        _uiState.update { it.copy(text = text) }
        renderPreview()
    }

    fun toggleDate() {
        _uiState.update { it.copy(showDate = !it.showDate) }
        renderPreview()
    }

    fun persistPendingForDetailEdit(sessionId: String) {
        val s = _uiState.value
        PendingCollageStore.write(
            getApplication(), sessionId,
            PendingCollageParams(
                filterId = s.selectedFilter,
                frameColorId = s.selectedFrameColor.id,
                text = s.text,
                showDate = s.showDate,
                order = s.order,
            ),
        )
    }

    private fun renderPreview() {
        val s = _uiState.value
        val layoutId = lastFrameLayoutId ?: return
        if (s.imagePaths.isEmpty()) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val orderedPaths = s.order.mapNotNull { idx -> s.imagePaths.getOrNull(idx) }
                    val bitmaps = orderedPaths.mapNotNull { BitmapDecoding.decodeSampled(it, reqSize = 720) }
                    val dateText = if (s.showDate) s.dateString else null
                    val style = FrameLayouts.byId(layoutId)
                    CollageRenderer.render(
                        frameStyle = style,
                        backgroundColor = s.selectedFrameColor.color,
                        bitmaps = bitmaps,
                        filterId = s.selectedFilter,
                        text = s.text.takeIf { it.isNotBlank() },
                        dateText = dateText,
                        targetWidth = 720,
                    )
                }
            }.onSuccess { bmp ->
                _uiState.update { it.copy(isLoading = false, preview = bmp) }
            }.onFailure { t ->
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message) }
            }
        }
    }

    suspend fun renderFinalAndSave(sessionId: String): String {
        val s = _uiState.value
        val params = PendingCollageParams(
            filterId = s.selectedFilter,
            frameColorId = s.selectedFrameColor.id,
            text = s.text,
            showDate = s.showDate,
            order = s.order,
        )
        return CollageFinalize.finalize(
            getApplication(), sessionId,
            lastFrameType ?: error("frameType missing"),
            lastFrameLayoutId ?: error("layoutId missing"),
            lastSelectedIndexes,
            params,
        )
    }
}
