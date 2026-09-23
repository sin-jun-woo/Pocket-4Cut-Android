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
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.domain.model.PhotoAdjustments
import com.pocket4cut.domain.model.PhotoCrop
import com.pocket4cut.domain.model.ResultRecord
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.frame.CropMath
import com.pocket4cut.frame.CropRect
import com.pocket4cut.frame.CustomFrameDesign
import com.pocket4cut.frame.FilterDefs
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameColor
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.FrameStyle
import com.pocket4cut.frame.FrameTheme
import com.pocket4cut.frame.RenderSnapshot
import com.pocket4cut.frame.PhotoCropTransform
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class PhotoSlotAdjustment(
    val quarterTurnsClockwise: Int = 0,
    val isFlippedHorizontally: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val crop: PhotoCrop = PhotoCrop(),
) {
    val isNeutral: Boolean
        get() = quarterTurnsClockwise == 0 &&
            !isFlippedHorizontally &&
            brightness == 0f &&
            contrast == 1f &&
            saturation == 1f &&
            crop == PhotoCrop()

    companion object {
        val neutral = PhotoSlotAdjustment()
    }
}

data class DetailEditUiState(
    val isRendering: Boolean = false,
    val errorMessage: String? = null,
    val selectedSlotIndex: Int = 0,
    val slotAdjustments: List<PhotoSlotAdjustment> = emptyList(),
    val collagePreviewImages: List<Bitmap> = emptyList(),
    val hasChanges: Boolean = false,
)

class DetailEditViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = FileImageStorage(app.applicationContext)
    private val sessions = SessionDocumentRepository(app.applicationContext)
    private val _uiState = MutableStateFlow(DetailEditUiState())
    val uiState: StateFlow<DetailEditUiState> = _uiState

    private var baseOrderedImages: List<Bitmap> = emptyList()
    private var imagePaths: List<String> = emptyList()
    private var photoIds: List<String> = emptyList()
    private var layoutVersion: Int = 2
    private var dateText: String = ""
    private var persistJob: Job? = null
    private val persistMutex = Mutex()
    private var frameType: FrameType = FrameType.FOUR_CUT
    private var frameStyle: FrameStyle = FrameLayouts.defaultForSlots(4)
    private var theme: FrameTheme = FrameCatalog.themes(FrameType.FOUR_CUT).first()
    private var frameColor: FrameColor = FrameColors.all.first()
    private var globalFilter: FilterId = FilterId.ORIGINAL
    private var customText: String = ""
    private var showDate: Boolean = false
    private var sessionId: String = ""
    private var selectedIndexes: List<Int> = emptyList()
    private var textFontSize: Float = 16f
    private var dateFontSize: Float = 16f
    private var captionFontName: String? = null
    private var captionColorRGB: Long? = null
    private var customFrameDesign: CustomFrameDesign? = null

    private var initialized = false
    private var previewJob: Job? = null
    private var previewGeneration = 0L
    private var publishedAdjustments: List<PhotoSlotAdjustment> = emptyList()
    private val previewMutex = Mutex()
    private val renderMutex = Mutex()

    // Preview bitmaps published through uiState can outlive the latest emission while Compose
    // finishes drawing an earlier frame. Never recycle a published bitmap manually; let the
    // runtime reclaim it after every UI consumer has released its reference.

    fun initialize(
        baseImages: List<Bitmap>,
        imagePaths: List<String>,
        photoIds: List<String>,
        initialAdjustments: Map<String, PhotoAdjustments>,
        layoutVersion: Int,
        dateText: String,
        frameType: FrameType,
        frameStyle: FrameStyle,
        theme: FrameTheme,
        frameColor: FrameColor,
        globalFilter: FilterId,
        customText: String,
        showDate: Boolean,
        sessionId: String,
        selectedIndexes: List<Int>,
        textFontSize: Float = 16f,
        dateFontSize: Float = 16f,
        captionFontName: String? = null,
        captionColorRGB: Long? = null,
        customFrameDesign: CustomFrameDesign? = null,
    ) {
        if (initialized) return
        initialized = true

        this.baseOrderedImages = baseImages
        this.imagePaths = imagePaths
        this.photoIds = photoIds
        this.layoutVersion = layoutVersion
        this.dateText = dateText
        this.frameType = frameType
        this.frameStyle = frameStyle
        this.theme = theme
        this.frameColor = frameColor
        this.globalFilter = globalFilter
        this.customText = customText
        this.showDate = showDate
        this.sessionId = sessionId
        this.selectedIndexes = selectedIndexes
        this.textFontSize = textFontSize
        this.dateFontSize = dateFontSize
        this.captionFontName = captionFontName
        this.captionColorRGB = captionColorRGB
        this.customFrameDesign = customFrameDesign

        val adjustments = photoIds.map { id ->
            initialAdjustments[id]?.let { adj ->
                PhotoSlotAdjustment(
                    quarterTurnsClockwise = ((adj.rotationDegrees / 90) % 4 + 4) % 4,
                    isFlippedHorizontally = adj.flipHorizontal,
                    brightness = adj.brightness,
                    contrast = adj.contrast,
                    saturation = adj.saturation,
                    crop = adj.crop,
                )
            } ?: PhotoSlotAdjustment.neutral
        }
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
        if (_uiState.value.isRendering) return
        val idx = _uiState.value.selectedSlotIndex
        if (idx !in _uiState.value.slotAdjustments.indices) return
        val next = _uiState.value.slotAdjustments.toMutableList().also { it[idx] = PhotoSlotAdjustment.neutral }
        _uiState.update { it.copy(slotAdjustments = next, hasChanges = false) }
        rebuildPreviewImage(idx)
        schedulePersist()
    }

    fun resetAllSlots() {
        if (_uiState.value.isRendering) return
        val count = _uiState.value.slotAdjustments.size
        if (count == 0) return
        val next = List(count) { PhotoSlotAdjustment.neutral }
        _uiState.update { it.copy(slotAdjustments = next, hasChanges = false) }
        rebuildAllPreviewImages()
        schedulePersist()
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

    /** Updates the crop preview without scheduling disk work for every pointer event. */
    fun previewCurrentCrop(crop: PhotoCrop) = updateCurrentSlot(
        transform = { it.copy(crop = crop) },
        rebuildProcessedImage = false,
        persist = false,
    )

    /** Commits the final value at gesture end or after an accessibility action. */
    fun commitCurrentCrop(crop: PhotoCrop) = updateCurrentSlot(
        transform = { it.copy(crop = crop) },
        rebuildProcessedImage = false,
        persist = true,
    )

    fun resetCurrentCrop() = commitCurrentCrop(PhotoCrop())

    /**
     * Renders the final collage at full resolution, saves to disk,
     * and returns the result file path.
     */
    suspend fun renderFinalCollage(): String {
        check(renderMutex.tryLock()) { "이미 결과 이미지를 만들고 있습니다." }
        _uiState.update { it.copy(isRendering = true) }
        return try {
            previewJob?.cancelAndJoin()
            persistJob?.cancelAndJoin()
            withContext(Dispatchers.IO) {
                val renderContext = currentCoroutineContext()
                val document = persistAdjustments()
                val snapshot = RenderSnapshot.from(document, sessions, frameType)
                val bgColor = snapshot.customFrameDesign?.resolvedFillColor ?: snapshot.frameColor.color
                val seasonalArt = snapshot.customFrameDesign?.resolvedSeason?.let {
                    com.pocket4cut.frame.rendering.SeasonalStickerArt.load(getApplication(), it)
                }
                val input = CollageRenderer.Input(
                    images = emptyList(),
                    slotImageProvider = { index, slotWidth, slotHeight ->
                        renderContext.ensureActive()
                        val id = snapshot.photoIdsInOrder[index]
                        val adj = snapshot.adjustmentsByPhotoId[id] ?: PhotoAdjustments()
                        processSlotImageFromPath(
                            path = snapshot.imagePathsInOrder[index],
                            adj = PhotoSlotAdjustment(
                                quarterTurnsClockwise = ((adj.rotationDegrees / 90) % 4 + 4) % 4,
                                isFlippedHorizontally = adj.flipHorizontal,
                                brightness = adj.brightness,
                                contrast = adj.contrast,
                                saturation = adj.saturation,
                                crop = adj.crop,
                            ),
                            filter = snapshot.filterId,
                            slotWidth = slotWidth,
                            slotHeight = slotHeight,
                            checkCancelled = renderContext::ensureActive,
                        )
                    },
                    cropTransforms = snapshot.cropTransformsInOrder,
                    recycleProvidedImages = true,
                    frameStyle = snapshot.frameStyle,
                    theme = snapshot.theme,
                    overrideBackground = bgColor,
                    backgroundGradient = snapshot.frameColor.gradientStops,
                    customFrameDesign = snapshot.customFrameDesign,
                    customDecorations = snapshot.customFrameDesign?.decorations ?: emptyList(),
                    filterId = FilterId.ORIGINAL,
                    text = snapshot.caption.takeIf { it.isNotBlank() },
                    dateString = snapshot.dateText,
                    textFontSize = snapshot.textFontSize,
                    dateFontSize = snapshot.dateFontSize,
                    textColorRGB = snapshot.captionColorRgb,
                    captionFontName = snapshot.captionFontName,
                    context = getApplication(),
                    layoutVersion = snapshot.layoutVersion,
                    seasonalArt = seasonalArt,
                )
                val result = CollageRenderer.render(input)
                try {
                    renderContext.ensureActive()
                    val resultId = UUID.randomUUID().toString()
                    val record = ResultRecord(
                        resultId = resultId,
                        sourceRevision = snapshot.revision,
                        path = "results/${sessionId}_${resultId}.jpg",
                        width = result.width,
                        height = result.height,
                        createdAt = System.currentTimeMillis(),
                    )
                    sessions.prepareResultPublication(sessionId, snapshot.revision, record)
                    val path = storage.saveImmutableResult(result, sessionId, resultId)
                    check(File(path).name == "${sessionId}_${resultId}.jpg")
                    sessions.completeResultPublication(sessionId, resultId)
                    path
                } finally {
                    result.recycle()
                }
            }
        } finally {
            _uiState.update { it.copy(isRendering = false) }
            renderMutex.unlock()
        }
    }

    /* ── Private helpers ────────────────────────────────────────────── */

    private inline fun updateCurrentSlot(
        rebuildProcessedImage: Boolean = true,
        persist: Boolean = true,
        transform: (PhotoSlotAdjustment) -> PhotoSlotAdjustment,
    ) {
        if (_uiState.value.isRendering) return
        val idx = _uiState.value.selectedSlotIndex
        if (idx !in _uiState.value.slotAdjustments.indices) return
        val next = _uiState.value.slotAdjustments.toMutableList().also { it[idx] = transform(it[idx]) }
        _uiState.update { it.copy(slotAdjustments = next, hasChanges = !next[idx].isNeutral) }
        if (rebuildProcessedImage) rebuildPreviewImage(idx)
        if (persist) schedulePersist()
    }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(180)
            runCatching { persistAdjustments() }.onFailure { cause ->
                if (cause is kotlinx.coroutines.CancellationException) throw cause
                _uiState.update { it.copy(errorMessage = cause.message ?: "보정값을 저장하지 못했습니다.") }
            }
        }
    }

    suspend fun leave(onSaved: () -> Unit) {
        persistJob?.cancelAndJoin()
        persistAdjustments()
        onSaved()
    }

    private suspend fun persistAdjustments(): SessionDocument = persistMutex.withLock {
        val mapping = photoIds.mapIndexed { index, id ->
            val adj = _uiState.value.slotAdjustments.getOrElse(index) { PhotoSlotAdjustment.neutral }
            id to PhotoAdjustments(
                rotationDegrees = adj.quarterTurnsClockwise * 90,
                flipHorizontal = adj.isFlippedHorizontally,
                brightness = adj.brightness,
                contrast = adj.contrast,
                saturation = adj.saturation,
                crop = adj.crop,
            )
        }.toMap()
        val current = sessions.getById(sessionId) ?: error("편집 작업을 찾지 못했습니다.")
        if (current.draft.adjustmentsByPhotoId == mapping && current.stage == SessionStage.DETAIL) return@withLock current
        sessions.update(sessionId, current.revision) { doc ->
            doc.copy(stage = SessionStage.DETAIL, draft = doc.draft.copy(adjustmentsByPhotoId = mapping))
        }
    }

    private fun rebuildAllPreviewImages() {
        val generation = ++previewGeneration
        previewJob?.cancel()
        val adjustments = _uiState.value.slotAdjustments.toList()
        val previousImages = _uiState.value.collagePreviewImages
        val previousAdjustments = publishedAdjustments
        previewJob = viewModelScope.launch {
            // The outer scope owns unpublished results even if withContext discards its return
            // value on cancellation while dispatching back to Main.
            val owned = mutableListOf<Bitmap>()
            var published = false
            try {
                val results = withContext(Dispatchers.Default) {
                    previewMutex.withLock {
                        val context = currentCoroutineContext()
                        baseOrderedImages.mapIndexed { index, base ->
                            context.ensureActive()
                            val adj = adjustments.getOrElse(index) { PhotoSlotAdjustment.neutral }
                            previousImages.getOrNull(index)?.takeIf {
                                previousAdjustments.getOrNull(index) == adj
                            } ?: processImage(base, adj, context::ensureActive).also(owned::add)
                        }
                    }
                }
                currentCoroutineContext().ensureActive()
                if (generation != previewGeneration) return@launch
                _uiState.update { it.copy(collagePreviewImages = results) }
                publishedAdjustments = adjustments
                published = true
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                _uiState.update { it.copy(errorMessage = cause.message ?: "미리보기를 만들지 못했습니다.") }
            } finally {
                if (!published) owned.forEach { it.recycle() }
            }
        }
    }

    private fun rebuildPreviewImage(index: Int) {
        if (index in baseOrderedImages.indices) rebuildAllPreviewImages()
    }

    /** Pipeline: rotation → global filter → per-slot color adjustments. */
    private fun processImage(source: Bitmap, adj: PhotoSlotAdjustment, checkCancelled: () -> Unit): Bitmap =
        processOwnedImage(source.copy(Bitmap.Config.ARGB_8888, false), adj, globalFilter, checkCancelled)

    /** Full-resolution variant that decodes from disk. */
    private fun processImageFromPath(
        path: String,
        adj: PhotoSlotAdjustment,
        filter: FilterId,
        checkCancelled: () -> Unit,
    ): Bitmap {
        val decoded = BitmapDecoding.decodeSampled(path, reqSize = 3072, maxPixels = 6_000_000L)
            ?: error("Failed to decode: $path")
        return processOwnedImage(decoded, adj, filter, checkCancelled)
    }

    /**
     * Region-decodes only the visible original area for a non-neutral crop. The exact crop is baked
     * into this temporary render bitmap, so the renderer receives a neutral replacement transform.
     * Unsupported formats and decoder failures retain the full-image sampled path and crop data.
     */
    private fun processSlotImageFromPath(
        path: String,
        adj: PhotoSlotAdjustment,
        filter: FilterId,
        slotWidth: Int,
        slotHeight: Int,
        checkCancelled: () -> Unit,
    ): CollageRenderer.ProvidedSlotImage {
        val originalTransform = PhotoCropTransform(
            crop = adj.crop,
            quarterTurnsClockwise = adj.quarterTurnsClockwise,
            flipHorizontal = adj.isFlippedHorizontally,
        )
        if (adj.crop == PhotoCrop()) {
            return CollageRenderer.ProvidedSlotImage(
                bitmap = processImageFromPath(path, adj, filter, checkCancelled),
                cropTransform = originalTransform,
            )
        }

        checkCancelled()
        val dimensions = BitmapDecoding.orientedDimensions(path)
        val turns = ((adj.quarterTurnsClockwise % 4) + 4) % 4
        val displayedWidth = if (turns % 2 == 0) dimensions?.width else dimensions?.height
        val displayedHeight = if (turns % 2 == 0) dimensions?.height else dimensions?.width
        val regionBitmap = if (displayedWidth != null && displayedHeight != null) {
            val visible = CropMath.visibleSourceRect(
                imageWidth = displayedWidth.toFloat(),
                imageHeight = displayedHeight.toFloat(),
                viewport = CropRect(0f, 0f, slotWidth.toFloat(), slotHeight.toFloat()),
                transform = originalTransform,
            )
            BitmapDecoding.decodeOrientedCropSampled(
                path = path,
                orientedCrop = BitmapDecoding.NormalizedImageRect(
                    visible.left,
                    visible.top,
                    visible.right,
                    visible.bottom,
                ),
                maxLongEdge = maxOf(3072, slotWidth, slotHeight),
                maxPixels = 6_000_000L,
            )
        } else null

        if (regionBitmap == null) {
            return CollageRenderer.ProvidedSlotImage(
                bitmap = processImageFromPath(path, adj, filter, checkCancelled),
                cropTransform = originalTransform,
            )
        }
        return CollageRenderer.ProvidedSlotImage(
            bitmap = processOwnedImage(regionBitmap, adj, filter, checkCancelled),
            cropTransform = PhotoCropTransform(),
        )
    }

    private fun processOwnedImage(
        source: Bitmap,
        adj: PhotoSlotAdjustment,
        filter: FilterId,
        checkCancelled: () -> Unit,
    ): Bitmap {
        var bmp = source
        try {
            checkCancelled()
            repeat(adj.quarterTurnsClockwise) {
                val rotated = BitmapAdjustments.rotate90(bmp)
                if (rotated !== bmp) bmp.recycle()
                bmp = rotated
                checkCancelled()
            }

            if (adj.isFlippedHorizontally) {
                val flipped = BitmapAdjustments.flipHorizontal(bmp)
                if (flipped !== bmp) bmp.recycle()
                bmp = flipped
                checkCancelled()
            }

            FilterDefs.colorFilter(filter)?.let { cf ->
                val filtered = applyColorFilter(bmp, cf)
                bmp.recycle()
                bmp = filtered
                checkCancelled()
            }

            if (adj.brightness != 0f || adj.contrast != 1f || adj.saturation != 1f) {
                val adjusted = BitmapAdjustments.applyColorAdjustments(bmp, adj.brightness, adj.contrast, adj.saturation)
                if (adjusted !== bmp) bmp.recycle()
                bmp = adjusted
                checkCancelled()
            }

            return bmp
        } catch (cause: Throwable) {
            bmp.recycle()
            throw cause
        }
    }

    private fun applyColorFilter(source: Bitmap, colorFilter: ColorMatrixColorFilter): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            paint.colorFilter = colorFilter
            canvas.drawBitmap(source, 0f, 0f, paint)
            return out
        } catch (cause: Throwable) {
            out.recycle()
            throw cause
        }
    }

    override fun onCleared() {
        previewJob?.cancel()
        super.onCleared()
    }
}
