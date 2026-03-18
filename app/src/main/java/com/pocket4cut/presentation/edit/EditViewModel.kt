package com.pocket4cut.presentation.edit

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.frame.FrameDefinitions
import com.pocket4cut.frame.FrameTheme
import com.pocket4cut.frame.RenderFilter
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
    val filter: RenderFilter = RenderFilter.SOFT,
    val text: String = "",
    val showDate: Boolean = true,
    val preview: Bitmap? = null,
)

class EditViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = FileImageStorage(app.applicationContext)

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState

    private var lastFrameType: FrameType? = null
    private var lastTheme: FrameTheme? = null

    fun init(
        frameType: FrameType,
        sessionId: String,
        selectedIndexes: List<Int>,
        themeId: String,
    ) {
        lastFrameType = frameType
        lastTheme = FrameDefinitions.byId(themeId)
        _uiState.update { it.copy(isLoading = true, errorMessage = null, preview = null) }
        viewModelScope.launch {
            runCatching { storage.getCapturePaths(sessionId) }
                .onSuccess { allPaths ->
                    val picked = selectedIndexes.mapNotNull { idx -> allPaths.getOrNull(idx) }
                    val order = picked.indices.toList()
                    _uiState.update { it.copy(isLoading = false, imagePaths = picked, order = order) }
                    renderPreview(frameType = frameType, themeId = themeId)
                }
                .onFailure { t ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "불러오기 실패") }
                }
        }
    }

    fun setFilter(filter: RenderFilter, frameType: FrameType, themeId: String) {
        _uiState.update { it.copy(filter = filter) }
        renderPreview(frameType = frameType, themeId = themeId)
    }

    fun setText(text: String, frameType: FrameType, themeId: String) {
        _uiState.update { it.copy(text = text) }
        renderPreview(frameType = frameType, themeId = themeId)
    }

    fun toggleDate(frameType: FrameType, themeId: String) {
        _uiState.update { it.copy(showDate = !it.showDate) }
        renderPreview(frameType = frameType, themeId = themeId)
    }

    fun swap(left: Int, right: Int, frameType: FrameType, themeId: String) {
        _uiState.update { state ->
            val mutable = state.order.toMutableList()
            val li = mutable.indexOf(left)
            val ri = mutable.indexOf(right)
            if (li >= 0 && ri >= 0) {
                val tmp = mutable[li]
                mutable[li] = mutable[ri]
                mutable[ri] = tmp
            }
            state.copy(order = mutable)
        }
        renderPreview(frameType = frameType, themeId = themeId)
    }

    private fun renderPreview(frameType: FrameType, themeId: String) {
        val snapshot = _uiState.value
        val theme = FrameDefinitions.byId(themeId)
        if (theme == null) return
        lastFrameType = frameType
        lastTheme = theme
        if (snapshot.imagePaths.isEmpty()) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val orderedPaths = snapshot.order.mapNotNull { idx -> snapshot.imagePaths.getOrNull(idx) }
                    val bitmaps = orderedPaths.mapNotNull { path ->
                        decodeSampledBitmap(path, reqSize = 720)
                    }
                    val dateText = if (snapshot.showDate) todayString() else null
                    CollageRenderer.render(
                        frameType = frameType,
                        theme = theme,
                        bitmaps = bitmaps,
                        filter = snapshot.filter,
                        text = snapshot.text.takeIf { it.isNotBlank() },
                        dateText = dateText,
                        targetWidth = 720,
                    )
                }
            }.onSuccess { bmp ->
                _uiState.update { it.copy(isLoading = false, preview = bmp) }
            }.onFailure { t ->
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "렌더링 실패") }
            }
        }
    }

    private fun todayString(): String =
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

    private fun decodeSampledBitmap(path: String, reqSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    suspend fun renderFinalAndSave(sessionId: String): String = withContext(Dispatchers.IO) {
        val snapshot = _uiState.value
        val frameType = lastFrameType ?: error("frameType missing")
        val theme = lastTheme ?: error("theme missing")

        val orderedPaths = snapshot.order.mapNotNull { idx -> snapshot.imagePaths.getOrNull(idx) }
        val bitmaps = orderedPaths.mapNotNull { path ->
            decodeSampledBitmap(path, reqSize = 1400)
        }
        val dateText = if (snapshot.showDate) todayString() else null
        val result = CollageRenderer.render(
            frameType = frameType,
            theme = theme,
            bitmaps = bitmaps,
            filter = snapshot.filter,
            text = snapshot.text.takeIf { it.isNotBlank() },
            dateText = dateText,
            targetWidth = 1920,
        )
        storage.saveResult(result, sessionId)
    }
}

