package com.pocket4cut.presentation.detailEdit

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.core.util.BitmapAdjustments
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.data.local.SessionRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.domain.model.PhotoSession
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.frame.FilterDefs
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameColor
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.FrameTheme
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhotoSlotAdjustment(
    val quarterTurnsClockwise: Int = 0,
    val isFlippedHorizontally: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
) {
    val isNeutral: Boolean
        get() = quarterTurnsClockwise == 0 &&
            !isFlippedHorizontally &&
            brightness == 0f &&
            contrast == 1f &&
            saturation == 1f

    companion object {
        val neutral = PhotoSlotAdjustment()
    }
}

data class DetailEditUiState(
    val isRendering: Boolean = false,
    val selectedSlotIndex: Int = 0,
    val slotAdjustments: List<PhotoSlotAdjustment> = emptyList(),
    val collagePreviewImages: List<Bitmap> = emptyList(),
    val hasChanges: Boolean = false,
)

class DetailEditViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = FileImageStorage(app.applicationContext)
    private val _uiState = MutableStateFlow(DetailEditUiState())
    val uiState: StateFlow<DetailEditUiState> = _uiState

    private var baseOrderedImages: List<Bitmap> = emptyList()
    private var imagePaths: List<String> = emptyList()
    private var frameType: FrameType = FrameType.FOUR_CUT
    private var frameStyle: FrameStyle = FrameLayouts.defaultForSlots(4)
    private var theme: FrameTheme = FrameCatalog.themes(FrameType.FOUR_CUT).first()
    private var frameColor: FrameColor = FrameColors.all.first()
    private var globalFilter: FilterId = FilterId.ORIGINAL
    private var customText: String = ""
    private var showDate: Boolean = false
    private var sessionId: String = ""
    private var selectedIndexes: List<Int> = emptyList()

    private var initialized = false
    private val slotJobs = mutableMapOf<Int, Job>()

    fun initialize(
        baseImages: List<Bitmap>,
        imagePaths: List<String>,
        frameType: FrameType,
        frameStyle: FrameStyle,
        theme: FrameTheme,
        frameColor: FrameColor,
        globalFilter: FilterId,
        customText: String,
        showDate: Boolean,
        sessionId: String,
        selectedIndexes: List<Int>,
    ) {
        if (initialized) return
        initialized = true

        this.baseOrderedImages = baseImages
        this.imagePaths = imagePaths
        this.frameType = frameType
        this.frameStyle = frameStyle
        this.theme = theme
        this.frameColor = frameColor
        this.globalFilter = globalFilter
        this.customText = customText
        this.showDate = showDate
        this.sessionId = sessionId
        this.selectedIndexes = selectedIndexes

        val adjustments = List(baseImages.size) { PhotoSlotAdjustment.neutral }
        _uiState.value = DetailEditUiState(slotAdjustments = adjustments)
        rebuildAllPreviewImages()
    }

    /* ── Public actions ─────────────────────────────────────────────── */

    fun selectSlot(index: Int) {
        if (index !in baseOrderedImages.indices) return
        _uiState.update {
            it.copy(
                selectedSlotIndex = index,
                hasChanges = it.slotAdjustments.getOrNull(index)?.isNeutral?.not() ?: false,
            )
        }
    }

    fun resetCurrentSlot() {
        val idx = _uiState.value.selectedSlotIndex
        if (idx !in _uiState.value.slotAdjustments.indices) return
        val next = _uiState.value.slotAdjustments.toMutableList().also { it[idx] = PhotoSlotAdjustment.neutral }
        _uiState.update { it.copy(slotAdjustments = next, hasChanges = false) }
        rebuildPreviewImage(idx)
    }

    fun resetAllSlots() {
        val count = _uiState.value.slotAdjustments.size
        if (count == 0) return
        val next = List(count) { PhotoSlotAdjustment.neutral }
        _uiState.update { it.copy(slotAdjustments = next, hasChanges = false) }
        rebuildAllPreviewImages()
    }

    fun rotateCurrentSlot() = updateCurrentSlot {
        it.copy(quarterTurnsClockwise = (it.quarterTurnsClockwise + 1) % 4)
    }

    fun flipCurrentHorizontally() = updateCurrentSlot {
        it.copy(isFlippedHorizontally = !it.isFlippedHorizontally)
    }

    fun setBrightness(modelValue: Float) = updateCurrentSlot {
        it.copy(brightness = modelValue.coerceIn(-0.35f, 0.35f))
    }

    fun setContrast(modelValue: Float) = updateCurrentSlot {
        it.copy(contrast = modelValue.coerceIn(0.5f, 1.5f))
    }

    fun setSaturation(modelValue: Float) = updateCurrentSlot {
        it.copy(saturation = modelValue.coerceIn(0f, 2f))
    }

    /**
     * Renders the final collage at full resolution, saves to disk,
     * and returns the result file path.
     */
    suspend fun renderFinalCollage(): String {
        _uiState.update { it.copy(isRendering = true) }
        return try {
            withContext(Dispatchers.IO) {
                val adjustments = _uiState.value.slotAdjustments
                val processedBitmaps = imagePaths.mapIndexed { i, path ->
                    processImageFromPath(path, adjustments.getOrElse(i) { PhotoSlotAdjustment.neutral })
                }
                val dateString = if (showDate) {
                    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
                } else {
                    null
                }
                val outputWidth = if (frameStyle.id == FrameLayoutId.FOUR_VERTICAL) 1650 else 1920
                val result = try {
                    CollageRenderer.render(
                        images = processedBitmaps,
                        frameStyle = frameStyle,
                        theme = theme,
                        overrideBackground = frameColor.color,
                        filterId = FilterId.ORIGINAL,
                        text = customText.takeIf { it.isNotBlank() },
                        dateString = dateString,
                        outputWidth = outputWidth,
                    )
                } finally {
                    processedBitmaps.forEach { it.recycle() }
                }
                try {
                    val path = storage.saveResult(result, sessionId)
                    val sessions = SessionRepository(getApplication())
                    sessions.upsert(
                        PhotoSession(
                            id = sessionId,
                            captureCount = frameType.captureCount,
                            selectedCount = frameType.selectCount,
                            imagePaths = imagePaths,
                            selectedIndexes = selectedIndexes,
                            frameId = frameStyle.id.name,
                            finalImagePath = path,
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                    path
                } finally {
                    result.recycle()
                }
            }
        } finally {
            _uiState.update { it.copy(isRendering = false) }
        }
    }

    /* ── Private helpers ────────────────────────────────────────────── */

    private inline fun updateCurrentSlot(transform: (PhotoSlotAdjustment) -> PhotoSlotAdjustment) {
        val idx = _uiState.value.selectedSlotIndex
        if (idx !in _uiState.value.slotAdjustments.indices) return
        val next = _uiState.value.slotAdjustments.toMutableList().also { it[idx] = transform(it[idx]) }
        _uiState.update { it.copy(slotAdjustments = next, hasChanges = !next[idx].isNeutral) }
        rebuildPreviewImage(idx)
    }

    private fun rebuildAllPreviewImages() {
        slotJobs.values.forEach { it.cancel() }
        slotJobs.clear()
        viewModelScope.launch {
            val adjustments = _uiState.value.slotAdjustments
            val results = withContext(Dispatchers.Default) {
                baseOrderedImages.mapIndexed { i, base ->
                    processImage(base, adjustments.getOrElse(i) { PhotoSlotAdjustment.neutral })
                }
            }
            if (!isActive) {
                results.forEach { it.recycle() }
                return@launch
            }
            _uiState.update { prev ->
                prev.collagePreviewImages.forEach { it.recycle() }
                prev.copy(collagePreviewImages = results)
            }
        }
    }

    private fun rebuildPreviewImage(index: Int) {
        val base = baseOrderedImages.getOrNull(index) ?: return
        val adj = _uiState.value.slotAdjustments.getOrElse(index) { PhotoSlotAdjustment.neutral }
        slotJobs[index]?.cancel()
        slotJobs[index] = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                processImage(base, adj)
            }
            if (!isActive) {
                result.recycle()
                return@launch
            }
            _uiState.update { prev ->
                if (index !in prev.collagePreviewImages.indices) {
                    result.recycle()
                    return@update prev
                }
                val mutable = prev.collagePreviewImages.toMutableList()
                mutable[index].recycle()
                mutable[index] = result
                prev.copy(collagePreviewImages = mutable)
            }
        }
    }

    /** Pipeline: rotation → global filter → per-slot color adjustments. */
    private fun processImage(source: Bitmap, adj: PhotoSlotAdjustment): Bitmap {
        var bmp = source.copy(Bitmap.Config.ARGB_8888, false)

        repeat(adj.quarterTurnsClockwise) {
            val rotated = BitmapAdjustments.rotate90(bmp)
            bmp.recycle()
            bmp = rotated
        }

        if (adj.isFlippedHorizontally) {
            val flipped = BitmapAdjustments.flipHorizontal(bmp)
            if (flipped !== bmp) bmp.recycle()
            bmp = flipped
        }

        FilterDefs.colorFilter(globalFilter)?.let { cf ->
            val filtered = applyColorFilter(bmp, cf)
            bmp.recycle()
            bmp = filtered
        }

        if (adj.brightness != 0f || adj.contrast != 1f || adj.saturation != 1f) {
            val adjusted = BitmapAdjustments.applyColorAdjustments(bmp, adj.brightness, adj.contrast, adj.saturation)
            bmp.recycle()
            bmp = adjusted
        }

        return bmp
    }

    /** Full-resolution variant that decodes from disk. */
    private fun processImageFromPath(path: String, adj: PhotoSlotAdjustment): Bitmap {
        var bmp = BitmapDecoding.decodeSampled(path, reqSize = 2400)
            ?: error("Failed to decode: $path")

        repeat(adj.quarterTurnsClockwise) {
            val rotated = BitmapAdjustments.rotate90(bmp)
            if (rotated !== bmp) bmp.recycle()
            bmp = rotated
        }

        if (adj.isFlippedHorizontally) {
            val flipped = BitmapAdjustments.flipHorizontal(bmp)
            if (flipped !== bmp) bmp.recycle()
            bmp = flipped
        }

        FilterDefs.colorFilter(globalFilter)?.let { cf ->
            val filtered = applyColorFilter(bmp, cf)
            bmp.recycle()
            bmp = filtered
        }

        if (adj.brightness != 0f || adj.contrast != 1f || adj.saturation != 1f) {
            val adjusted = BitmapAdjustments.applyColorAdjustments(bmp, adj.brightness, adj.contrast, adj.saturation)
            if (adjusted !== bmp) bmp.recycle()
            bmp = adjusted
        }

        return bmp
    }

    private fun applyColorFilter(source: Bitmap, colorFilter: ColorMatrixColorFilter): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = colorFilter
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    override fun onCleared() {
        slotJobs.values.forEach { it.cancel() }
        slotJobs.clear()
        super.onCleared()
        _uiState.value.collagePreviewImages.forEach { it.recycle() }
    }
}
