package com.pocket4cut.presentation.capture

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.pocket4cut.camera.CaptureEngine
import com.pocket4cut.core.util.Constants
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.presentation.settings.AppSettings
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

data class CaptureUiState(
    val phase: CapturePhase = CapturePhase.IDLE,
    val countdownRemaining: Int = AppSettings.countdownSeconds,
    val currentShot: Int = 0,
    val totalShots: Int = 0,
    val sessionId: String? = null,
    val flash: Boolean = false,
    val isFrontCamera: Boolean = AppSettings.preferFrontCamera,
    val errorMessage: String? = null,
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val zoomRatio: Float = 1f,
)

enum class CapturePhase {
    IDLE,
    READY,
    INITIALIZING,
    COUNTDOWN,
    CAPTURING,
    POST_SHOT_DELAY,
    PAUSED,
    COMPLETED,
    FAILED,
}

class CaptureViewModel(app: Application, private val savedState: SavedStateHandle) : AndroidViewModel(app) {
    private val engine = CaptureEngine(app.applicationContext)
    private val storage = FileImageStorage(app.applicationContext)
    private val sessions = SessionDocumentRepository(app.applicationContext)

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState

    private var captureJob: Job? = null
    private var sessionRevision: Long? = null
    private var pauseRequested = false
    private var boundLifecycleOwner: LifecycleOwner? = null
    private var boundPreviewView: PreviewView? = null
    private var cameraBound = false

    /** 카운트다운 중 수동 셔터 (남은 초는 버리고 즉시 촬영) */
    private val manualShutter = Channel<Unit>(Channel.CONFLATED)

    fun savedSessionId(): String? = savedState["captureSessionId"]

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (captureJob?.isActive == true && _uiState.value.phase in setOf(
                CapturePhase.COUNTDOWN, CapturePhase.CAPTURING, CapturePhase.POST_SHOT_DELAY,
            )) return
        if (cameraBound && boundLifecycleOwner === lifecycleOwner && boundPreviewView === previewView) return
        boundLifecycleOwner = lifecycleOwner
        boundPreviewView = previewView
        viewModelScope.launch {
            runCatching {
                val lensFacing = if (_uiState.value.isFrontCamera) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
                engine.bind(lifecycleOwner, previewView, lensFacing)
                cameraBound = true
                val range = engine.zoomRatioRange() ?: (1f to 1f)
                val z = engine.currentZoomRatio()?.coerceIn(range.first, range.second) ?: range.first
                _uiState.update {
                    it.copy(
                        phase = if (it.phase == CapturePhase.IDLE ||
                            (it.phase == CapturePhase.FAILED && it.sessionId == null)
                        ) CapturePhase.READY else it.phase,
                        errorMessage = null,
                        minZoom = range.first,
                        maxZoom = range.second,
                        zoomRatio = z,
                    )
                }
            }.onFailure { t ->
                if (t is CancellationException) throw t
                cameraBound = false
                _uiState.update { it.copy(phase = CapturePhase.FAILED, errorMessage = t.message ?: "카메라 연결에 실패했습니다.") }
            }
        }
    }

    fun switchCamera() {
        if (_uiState.value.phase !in setOf(CapturePhase.READY, CapturePhase.PAUSED, CapturePhase.FAILED)) return
        val lifecycleOwner = boundLifecycleOwner ?: return
        val previewView = boundPreviewView ?: return
        val previous = _uiState.value.isFrontCamera
        val phaseBefore = _uiState.value.phase
        _uiState.update { it.copy(isFrontCamera = !previous) }
        viewModelScope.launch {
            runCatching {
                val lensFacing = if (_uiState.value.isFrontCamera) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
                engine.bind(lifecycleOwner, previewView, lensFacing)
                cameraBound = true
                val range = engine.zoomRatioRange() ?: (1f to 1f)
                val resetZoom = range.first
                engine.setZoomRatio(resetZoom)
                val applied = engine.currentZoomRatio()?.coerceIn(range.first, range.second) ?: resetZoom
                _uiState.update {
                    it.copy(
                        phase = phaseBefore,
                        errorMessage = null,
                        minZoom = range.first,
                        maxZoom = range.second,
                        zoomRatio = applied,
                    )
                }
            }.onFailure { t ->
                cameraBound = false
                _uiState.update {
                    it.copy(
                        isFrontCamera = previous,
                        phase = CapturePhase.FAILED,
                        errorMessage = t.message ?: "카메라 전환에 실패했습니다.",
                    )
                }
                runCatching {
                    val fallbackFacing = if (previous) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                    engine.bind(lifecycleOwner, previewView, fallbackFacing)
                }
            }
        }
    }

    fun setZoomRatio(ratio: Float) {
        val s = _uiState.value
        val clamped = ratio.coerceIn(s.minZoom, s.maxZoom)
        engine.setZoomRatio(clamped)
        val applied = engine.currentZoomRatio() ?: clamped
        _uiState.update { it.copy(zoomRatio = applied.coerceIn(it.minZoom, it.maxZoom)) }
    }

    /** 카운트다운이 돌아가는 동안만 동작. 누르면 남은 대기 시간을 건너뛰고 바로 셔터. */
    fun onManualShutter() {
        if (_uiState.value.phase != CapturePhase.COUNTDOWN) return
        manualShutter.trySend(Unit)
    }

    fun start(frameType: FrameType) {
        if (captureJob?.isActive == true) return
        if (_uiState.value.phase !in setOf(CapturePhase.READY, CapturePhase.IDLE)) return
        val id = UUID.randomUUID().toString()
        pauseRequested = false
        drainManualShutter()
        _uiState.update { it.copy(phase = CapturePhase.INITIALIZING, errorMessage = null) }
        captureJob = viewModelScope.launch {
            try {
                val created = sessions.create(
                    SessionDocument(
                        sessionId = id,
                        createdAt = System.currentTimeMillis(),
                        captureCount = frameType.captureCount,
                        selectedCount = frameType.selectCount,
                    ),
                )
                sessionRevision = created.revision
                savedState["captureSessionId"] = id
                _uiState.update {
                    it.copy(
                        phase = if (pauseRequested) CapturePhase.PAUSED else CapturePhase.READY,
                        countdownRemaining = AppSettings.countdownSeconds,
                        currentShot = 0,
                        totalShots = frameType.captureCount,
                        sessionId = id,
                        flash = false,
                        errorMessage = null,
                    )
                }
                if (!pauseRequested) captureRemaining(id, frameType.captureCount, 0)
            } catch (_: CancellationException) {
                _uiState.update { it.copy(phase = CapturePhase.PAUSED, flash = false) }
            } catch (t: Exception) {
                _uiState.update {
                    it.copy(phase = if (pauseRequested) CapturePhase.PAUSED else CapturePhase.FAILED,
                        errorMessage = if (pauseRequested) null else t.message ?: "촬영에 실패했습니다.", flash = false)
                }
            }
        }
    }

    /** Restore an existing capture without starting the camera shutter. */
    fun restoreSession(sessionId: String, frameType: FrameType) {
        if (captureJob?.isActive == true || _uiState.value.sessionId == sessionId) return
        viewModelScope.launch {
            try {
                val document = sessions.getById(sessionId) ?: error("저장된 촬영을 찾지 못했습니다.")
                require(document.captureCount == frameType.captureCount)
                val recovered = recoverPublishedCaptures(document)
                sessionRevision = recovered.revision
                savedState["captureSessionId"] = sessionId
                _uiState.update {
                    it.copy(
                        sessionId = sessionId,
                        totalShots = recovered.captureCount,
                        currentShot = recovered.photos.size,
                        phase = if (recovered.photos.size >= recovered.captureCount) {
                            CapturePhase.COMPLETED
                        } else {
                            CapturePhase.PAUSED
                        },
                        errorMessage = null,
                    )
                }
            } catch (t: Exception) {
                _uiState.update { it.copy(phase = CapturePhase.FAILED, errorMessage = t.message) }
            }
        }
    }

    fun resume() {
        if (_uiState.value.phase !in setOf(CapturePhase.PAUSED, CapturePhase.FAILED)) return
        val id = _uiState.value.sessionId ?: return
        if (captureJob?.isActive == true) return
        pauseRequested = false
        drainManualShutter()
        _uiState.update { it.copy(phase = CapturePhase.INITIALIZING, errorMessage = null) }
        captureJob = viewModelScope.launch {
            try {
                val document = sessions.getById(id) ?: error("저장된 촬영을 찾지 못했습니다.")
                val recovered = recoverPublishedCaptures(document)
                sessionRevision = recovered.revision
                _uiState.update { it.copy(currentShot = recovered.photos.size, errorMessage = null) }
                if (pauseRequested) {
                    _uiState.update { it.copy(phase = CapturePhase.PAUSED) }
                } else {
                    captureRemaining(id, recovered.captureCount, recovered.photos.size)
                }
            } catch (_: CancellationException) {
                _uiState.update { it.copy(phase = CapturePhase.PAUSED, flash = false) }
            } catch (t: Exception) {
                _uiState.update { it.copy(phase = if (pauseRequested) CapturePhase.PAUSED else CapturePhase.FAILED,
                    errorMessage = if (pauseRequested) null else t.message, flash = false) }
            }
        }
    }

    private suspend fun captureRemaining(sessionId: String, total: Int, completed: Int) {
        if (completed >= total) {
            _uiState.update { it.copy(phase = CapturePhase.COMPLETED, currentShot = total) }
            return
        }
        for (i in (completed + 1)..total) {
            if (pauseRequested) break
            drainManualShutter()
            var sec = AppSettings.countdownSeconds
            while (sec > 0) {
                _uiState.update {
                    it.copy(
                        phase = CapturePhase.COUNTDOWN,
                        countdownRemaining = sec,
                        currentShot = i - 1,
                        totalShots = total,
                        flash = false,
                    )
                }
                val manual = withTimeoutOrNull(1000L) {
                    manualShutter.receive()
                }
                if (manual != null) break
                sec--
            }

            if (pauseRequested) break
            _uiState.update { it.copy(phase = CapturePhase.CAPTURING, flash = true) }
            val pending = storage.createPendingCaptureFile(sessionId)
            engine.takePictureToFile(pending)
            val published = storage.publishCaptureFile(pending, sessionId, i)
            val revision = sessionRevision ?: error("촬영 기록의 버전이 없습니다.")
            val saved = sessions.appendCapture(
                sessionId,
                revision,
                PhotoRef(
                    photoId = UUID.randomUUID().toString(),
                    path = "captures/$sessionId/${published.name}",
                    captureIndex = i - 1,
                ),
            )
            sessionRevision = saved.revision
            _uiState.update { it.copy(currentShot = i, flash = false) }
            if (pauseRequested) break
            _uiState.update { it.copy(phase = CapturePhase.POST_SHOT_DELAY) }
            delay(300)

            if (i != total) {
                delay(Constants.CAPTURE_INTERVAL_SECONDS * 1000L)
            }
        }
        _uiState.update {
            it.copy(phase = if (pauseRequested) CapturePhase.PAUSED else CapturePhase.COMPLETED, flash = false)
        }
    }

    private suspend fun recoverPublishedCaptures(initial: SessionDocument): SessionDocument {
        var current = initial
        val documented = current.photos.map { it.captureIndex }.toSet()
        val files = storage.getCapturePaths(current.sessionId)
        for (filePath in files) {
            val file = java.io.File(filePath)
            val index = file.name.removePrefix("cap_").removeSuffix(".jpg").toIntOrNull()?.minus(1) ?: continue
            if (index !in 0 until current.captureCount || index in documented ||
                current.photos.any { it.captureIndex == index }
            ) continue
            current = sessions.appendCapture(
                current.sessionId,
                current.revision,
                PhotoRef(UUID.randomUUID().toString(), "captures/${current.sessionId}/${file.name}", index),
            )
        }
        return current
    }

    fun pause() {
        if (_uiState.value.phase !in setOf(CapturePhase.INITIALIZING, CapturePhase.COUNTDOWN,
                CapturePhase.CAPTURING, CapturePhase.POST_SHOT_DELAY)) return
        pauseRequested = true
        drainManualShutter()
        if (_uiState.value.phase == CapturePhase.INITIALIZING) {
            // Keep the create/recover job alive so its document can be attached to the paused UI.
            _uiState.update { it.copy(phase = CapturePhase.PAUSED, flash = false) }
        } else if (_uiState.value.phase != CapturePhase.CAPTURING) {
            captureJob?.cancel()
            captureJob = null
            _uiState.update { it.copy(phase = CapturePhase.PAUSED, flash = false) }
        }
    }

    fun stop() = pause()

    private fun drainManualShutter() {
        while (manualShutter.tryReceive().isSuccess) { }
    }

    override fun onCleared() {
        super.onCleared()
        engine.unbind()
    }
}
