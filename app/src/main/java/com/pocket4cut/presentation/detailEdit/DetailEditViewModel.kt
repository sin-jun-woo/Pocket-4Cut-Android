package com.pocket4cut.presentation.detailEdit

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.core.util.BitmapAdjustments
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.presentation.edit.CollageFinalize
import com.pocket4cut.presentation.edit.PendingCollageStore
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SlotAdjust(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val rotationQuarters: Int = 0,
) {
    val isDefault: Boolean
        get() = brightness == 0f && contrast == 1f && saturation == 1f && rotationQuarters == 0
}

data class DetailEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val orderedPaths: List<String> = emptyList(),
    val selectedSlot: Int = 0,
    val slotAdjusts: List<SlotAdjust> = emptyList(),
    val preview: Bitmap? = null,
    val hasChanges: Boolean = false,
)

class DetailEditViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = FileImageStorage(app.applicationContext)

    private val _uiState = MutableStateFlow(DetailEditUiState())
    val uiState: StateFlow<DetailEditUiState> = _uiState

    private var lastFrameType: FrameType? = null
    private var lastLayoutId: FrameLayoutId? = null
    private var lastSelectedIndexes: List<Int> = emptyList()
    private var previewJob: Job? = null

    fun init(
        frameType: FrameType,
        sessionId: String,
        selectedIndexes: List<Int>,
        layoutId: String,
    ) {
        lastFrameType = frameType
        lastLayoutId = runCatching { FrameLayoutId.valueOf(layoutId) }.getOrNull()
        lastSelectedIndexes = selectedIndexes
        _uiState.value = DetailEditUiState(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val pending = PendingCollageStore.read(getApplication(), sessionId)
            if (pending == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "편집 정보를 찾을 수 없어요. 이전 화면에서 다시 진행해 줘.")
                }
                return@launch
            }
            runCatching { storage.getCapturePaths(sessionId) }
                .onSuccess { allPaths ->
                    val picked = selectedIndexes.mapNotNull { idx -> allPaths.getOrNull(idx) }
                    val ordered = pending.order.mapNotNull { i -> picked.getOrNull(i) }
                    if (ordered.isEmpty()) {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "사진 경로가 비어 있어.")
                        }
                        return@launch
                    }
                    val adjusts = List(ordered.size) { SlotAdjust() }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            orderedPaths = ordered,
                            selectedSlot = 0,
                            slotAdjusts = adjusts,
                            hasChanges = false,
                            errorMessage = null,
                        )
                    }
                    renderSelectedPreview()
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = t.message ?: "불러오기 실패",
                        )
                    }
                }
        }
    }

    fun selectSlot(index: Int) {
        val paths = _uiState.value.orderedPaths
        if (index !in paths.indices) return
        _uiState.update { it.copy(selectedSlot = index, hasChanges = computeHasChanges(it.slotAdjusts, index)) }
        renderSelectedPreview()
    }

    fun resetCurrent() {
        val s = _uiState.value
        val idx = s.selectedSlot
        if (idx !in s.slotAdjusts.indices) return
        val next = s.slotAdjusts.toMutableList().also { it[idx] = SlotAdjust() }
        _uiState.update { it.copy(slotAdjusts = next, hasChanges = false) }
        renderSelectedPreview()
    }

    fun setBrightness(v: Float) = updateCurrentSlot { it.copy(brightness = v.coerceIn(-0.35f, 0.35f)) }
    fun setContrast(v: Float) = updateCurrentSlot { it.copy(contrast = v.coerceIn(0.7f, 1.5f)) }
    fun setSaturation(v: Float) = updateCurrentSlot { it.copy(saturation = v.coerceIn(0f, 2f)) }

    fun rotateQuarter() = updateCurrentSlot {
        it.copy(rotationQuarters = (it.rotationQuarters + 1) % 4)
    }

    private fun computeHasChanges(adjusts: List<SlotAdjust>, slotIndex: Int): Boolean {
        val adj = adjusts.getOrNull(slotIndex) ?: return false
        return !adj.isDefault
    }

    private inline fun updateCurrentSlot(transform: (SlotAdjust) -> SlotAdjust) {
        val s = _uiState.value
        val idx = s.selectedSlot
        if (idx !in s.slotAdjusts.indices) return
        val next = s.slotAdjusts.toMutableList().also { it[idx] = transform(it[idx]) }
        _uiState.update { it.copy(slotAdjusts = next, hasChanges = computeHasChanges(next, idx)) }
        renderSelectedPreview()
    }

    private fun renderSelectedPreview() {
        val paths = _uiState.value.orderedPaths
        val idx = _uiState.value.selectedSlot
        val adj = _uiState.value.slotAdjusts.getOrNull(idx) ?: return
        val path = paths.getOrNull(idx) ?: return

        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    var bmp = BitmapDecoding.decodeSampled(path, reqSize = 720) ?: return@withContext null
                    try {
                        repeat(adj.rotationQuarters) {
                            val r = BitmapAdjustments.rotate90(bmp)
                            if (r !== bmp) bmp.recycle()
                            bmp = r
                        }
                        val out = BitmapAdjustments.applyColorAdjustments(
                            bmp,
                            adj.brightness,
                            adj.contrast,
                            adj.saturation,
                        )
                        if (out !== bmp) bmp.recycle()
                        out
                    } catch (e: Exception) {
                        bmp.recycle()
                        throw e
                    }
                }
            }.onSuccess { bmp ->
                if (!isActive) {
                    bmp?.recycle()
                    return@launch
                }
                _uiState.update { prev ->
                    prev.preview?.recycle()
                    prev.copy(isLoading = false, preview = bmp)
                }
            }.onFailure { t ->
                if (!isActive) return@launch
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "미리보기 실패") }
            }
        }
    }

    suspend fun applyAndFinish(sessionId: String): String {
        val frameType = lastFrameType ?: error("frameType 없음")
        val layoutId = lastLayoutId ?: error("layoutId 없음")
        val pending = PendingCollageStore.read(getApplication(), sessionId)
            ?: error("편집 정보 없음")
        val paths = _uiState.value.orderedPaths
        val adjusts = _uiState.value.slotAdjusts
        if (paths.size != adjusts.size) error("슬롯 불일치")

        withContext(Dispatchers.IO) {
            paths.forEachIndexed { i, path ->
                val adj = adjusts[i]
                var bmp = BitmapDecoding.decodeSampled(path, reqSize = 2400) ?: return@forEachIndexed
                try {
                    repeat(adj.rotationQuarters) {
                        val r = BitmapAdjustments.rotate90(bmp)
                        if (r !== bmp) bmp.recycle()
                        bmp = r
                    }
                    val out = BitmapAdjustments.applyColorAdjustments(
                        bmp,
                        adj.brightness,
                        adj.contrast,
                        adj.saturation,
                    )
                    if (out !== bmp) bmp.recycle()
                    storage.saveBitmapToPath(out, path)
                    out.recycle()
                } catch (e: Exception) {
                    bmp.recycle()
                    throw e
                }
            }
        }

        val resultPath = CollageFinalize.finalize(
            getApplication(),
            sessionId,
            frameType,
            layoutId,
            lastSelectedIndexes,
            pending,
        )
        PendingCollageStore.delete(getApplication(), sessionId)
        return resultPath
    }

    override fun onCleared() {
        previewJob?.cancel()
        super.onCleared()
        _uiState.value.preview?.recycle()
    }
}
