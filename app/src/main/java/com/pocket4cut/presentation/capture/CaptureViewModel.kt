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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.lang.ref.WeakReference
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
    val damagedCaptureIndex: Int? = null,
    val recoveryBlocked: Boolean = false,
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

sealed class CaptureRecoveryProblem(val completedCount: Int, message: String) : IOException(message)

class DamagedPublishedCapture(val index: Int, completedCount: Int) : CaptureRecoveryProblem(
    completedCount,
    "${index}번째 미기록 사진 파일이 손상되었습니다. 손상 파일을 격리한 뒤 이 컷을 다시 촬영할 수 있습니다.",
)

class RecordedCaptureUnavailable(val index: Int, completedCount: Int) : CaptureRecoveryProblem(
    completedCount,
    "이미 기록된 ${index}번째 촬영 원본이 없거나 손상되었습니다. 다른 사진과 완료본은 보존했으며 자동 재촬영은 중단했습니다.",
)

class CaptureSequenceMismatch(completedCount: Int) : CaptureRecoveryProblem(
    completedCount,
    "촬영 파일의 컷 순서가 기록과 다릅니다. 파일은 보존했으며 잘못된 순서로 이어 붙이지 않도록 촬영을 중단했습니다.",
)

class CaptureViewModel(app: Application, private val savedState: SavedStateHandle) : AndroidViewModel(app) {
    private val engine = CaptureEngine(app.applicationContext)
    private val storage = FileImageStorage(app.applicationContext)
    private val sessions = SessionDocumentRepository(app.applicationContext)
    private val recovery = CaptureFileRecovery(storage, sessions)

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState

    private var captureJob: Job? = null
    private var sessionRevision: Long? = null
    private var pauseRequested = false
    private var boundLifecycleOwner: WeakReference<LifecycleOwner>? = null
    private var boundPreviewView: WeakReference<PreviewView>? = null
    private var cameraBound = false
    private var pendingRebind = false
    private var deferredBindOnJob: Job? = null
    private var cameraBindJob: Job? = null
    private var cameraSwitchJob: Job? = null
    private var cameraBindRequest = 0L
    private val cameraBindMutex = Mutex()

    /** 카운트다운 중 수동 셔터 (남은 초는 버리고 즉시 촬영) */
    private val manualShutter = Channel<Unit>(Channel.CONFLATED)

    fun savedSessionId(): String? = savedState["captureSessionId"]

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val sameTarget = boundLifecycleOwner?.get() === lifecycleOwner &&
            boundPreviewView?.get() === previewView
        boundLifecycleOwner = WeakReference(lifecycleOwner)
        boundPreviewView = WeakReference(previewView)
        val activeCapture = captureJob?.takeIf { job -> job.isActive && _uiState.value.phase in setOf(
            CapturePhase.COUNTDOWN, CapturePhase.CAPTURING, CapturePhase.POST_SHOT_DELAY,
        ) }
        if (activeCapture != null) {
            pendingRebind = true
            if (deferredBindOnJob !== activeCapture) {
                deferredBindOnJob = activeCapture
                activeCapture.invokeOnCompletion {
                    viewModelScope.launch {
                        if (deferredBindOnJob !== activeCapture) return@launch
                        deferredBindOnJob = null
                        val latestOwner = boundLifecycleOwner?.get() ?: return@launch
                        val latestView = boundPreviewView?.get() ?: return@launch
                        bindCamera(latestOwner, latestView)
                    }
                }
            }
            return
        }
        if (sameTarget && !pendingRebind && (cameraBound || cameraBindJob?.isActive == true)) return
        pendingRebind = false
        val request = ++cameraBindRequest
        cameraBindJob?.cancel()
        cameraBindJob = viewModelScope.launch {
            cameraBindMutex.withLock {
                if (request != cameraBindRequest) return@withLock
                try {
                    val lensFacing = if (_uiState.value.isFrontCamera) {
                        CameraSelector.LENS_FACING_FRONT
                    } else CameraSelector.LENS_FACING_BACK
                    engine.bind(lifecycleOwner, previewView, lensFacing)
                    if (request != cameraBindRequest) return@withLock
                    cameraBound = true
                    val range = engine.zoomRatioRange() ?: (1f to 1f)
                    val z = engine.currentZoomRatio()?.coerceIn(range.first, range.second) ?: range.first
                    _uiState.update {
                        it.copy(
                            phase = if (it.phase == CapturePhase.IDLE ||
                                (it.phase == CapturePhase.FAILED && it.sessionId == null)
                            ) CapturePhase.READY else it.phase,
                            errorMessage = if (it.damagedCaptureIndex == null && !it.recoveryBlocked) null else it.errorMessage,
                            minZoom = range.first,
                            maxZoom = range.second,
                            zoomRatio = z,
                        )
                    }
                } catch (t: Exception) {
                    if (t is CancellationException) throw t
                    if (request != cameraBindRequest) return@withLock
                    cameraBound = false
                    _uiState.update { it.copy(phase = CapturePhase.FAILED,
                        errorMessage = t.message ?: "카메라 연결에 실패했습니다.") }
                }
            }
        }
    }

    fun switchCamera() {
        if (_uiState.value.phase !in setOf(CapturePhase.READY, CapturePhase.PAUSED, CapturePhase.FAILED)) return
        if (cameraBindJob?.isActive == true || cameraSwitchJob?.isActive == true) return
        val lifecycleOwner = boundLifecycleOwner?.get() ?: return
        val previewView = boundPreviewView?.get() ?: return
        val previous = _uiState.value.isFrontCamera
        cameraSwitchJob = viewModelScope.launch {
            cameraBindMutex.withLock {
                try {
                    val lensFacing = if (previous) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                    engine.bind(lifecycleOwner, previewView, lensFacing)
                    cameraBound = true
                    val range = engine.zoomRatioRange() ?: (1f to 1f)
                    val resetZoom = range.first
                    engine.setZoomRatio(resetZoom)
                    val applied = engine.currentZoomRatio()?.coerceIn(range.first, range.second) ?: resetZoom
                    _uiState.update {
                        it.copy(
                            isFrontCamera = !previous,
                            errorMessage = if (it.damagedCaptureIndex == null && !it.recoveryBlocked) null else it.errorMessage,
                            minZoom = range.first,
                            maxZoom = range.second,
                            zoomRatio = applied,
                        )
                    }
                } catch (t: Exception) {
                    if (t is CancellationException) throw t
                    cameraBound = false
                    _uiState.update {
                        it.copy(
                            phase = CapturePhase.FAILED,
                            errorMessage = t.message ?: "카메라 전환에 실패했습니다.",
                        )
                    }
                    try {
                        val fallbackFacing = if (previous) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        engine.bind(lifecycleOwner, previewView, fallbackFacing)
                    } catch (fallback: Exception) {
                        if (fallback is CancellationException) throw fallback
                    }
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
        if (cameraSwitchJob?.isActive == true) return
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
                        damagedCaptureIndex = null,
                        recoveryBlocked = false,
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
        captureJob = viewModelScope.launch {
            try {
                val document = sessions.getById(sessionId) ?: error("저장된 촬영을 찾지 못했습니다.")
                require(document.captureCount == frameType.captureCount)
                savedState["captureSessionId"] = sessionId
                _uiState.update { it.copy(
                    sessionId = sessionId,
                    totalShots = document.captureCount,
                    currentShot = document.photos.size,
                    phase = CapturePhase.INITIALIZING,
                    damagedCaptureIndex = null,
                    recoveryBlocked = false,
                ) }
                val recovered = recovery.recover(document)
                sessionRevision = recovered.revision
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
                        damagedCaptureIndex = null,
                        recoveryBlocked = false,
                    )
                }
            } catch (t: CaptureRecoveryProblem) {
                _uiState.update { it.copy(
                    sessionId = sessionId,
                    currentShot = t.completedCount,
                    phase = CapturePhase.FAILED,
                    damagedCaptureIndex = (t as? DamagedPublishedCapture)?.index,
                    recoveryBlocked = t !is DamagedPublishedCapture,
                    errorMessage = t.message,
                ) }
            } catch (_: CancellationException) {
                _uiState.update { it.copy(phase = CapturePhase.PAUSED, flash = false) }
            } catch (t: Exception) {
                _uiState.update { it.copy(phase = CapturePhase.FAILED, errorMessage = t.message) }
            }
        }
    }

    fun resume() {
        if (_uiState.value.phase !in setOf(CapturePhase.PAUSED, CapturePhase.FAILED)) return
        if (cameraSwitchJob?.isActive == true) return
        if (_uiState.value.damagedCaptureIndex != null || _uiState.value.recoveryBlocked) return
        val id = _uiState.value.sessionId ?: return
        if (captureJob?.isActive == true) return
        pauseRequested = false
        drainManualShutter()
        _uiState.update { it.copy(phase = CapturePhase.INITIALIZING, errorMessage = null) }
        captureJob = viewModelScope.launch {
            try {
                val document = sessions.getById(id) ?: error("저장된 촬영을 찾지 못했습니다.")
                val recovered = recovery.recover(document)
                sessionRevision = recovered.revision
                _uiState.update { it.copy(currentShot = recovered.photos.size, errorMessage = null,
                    damagedCaptureIndex = null, recoveryBlocked = false) }
                if (pauseRequested) {
                    _uiState.update { it.copy(phase = CapturePhase.PAUSED) }
                } else {
                    captureRemaining(id, recovered.captureCount, recovered.photos.size)
                }
            } catch (_: CancellationException) {
                _uiState.update { it.copy(phase = CapturePhase.PAUSED, flash = false) }
            } catch (t: CaptureRecoveryProblem) {
                _uiState.update { it.copy(phase = CapturePhase.FAILED, currentShot = t.completedCount,
                    damagedCaptureIndex = (t as? DamagedPublishedCapture)?.index,
                    recoveryBlocked = t !is DamagedPublishedCapture,
                    errorMessage = t.message, flash = false) }
            } catch (t: Exception) {
                _uiState.update { it.copy(phase = if (pauseRequested) CapturePhase.PAUSED else CapturePhase.FAILED,
                    errorMessage = if (pauseRequested) null else t.message, flash = false) }
            }
        }
    }

    /** Invoked only after the user chooses to preserve the damaged candidate and retake its slot. */
    fun quarantineDamagedCaptureAndResume() {
        val state = _uiState.value
        val id = state.sessionId ?: return
        val damagedIndex = state.damagedCaptureIndex ?: return
        if (state.phase != CapturePhase.FAILED || captureJob?.isActive == true) return
        pauseRequested = false
        drainManualShutter()
        _uiState.update { it.copy(phase = CapturePhase.INITIALIZING, errorMessage = null) }
        captureJob = viewModelScope.launch {
            try {
                storage.quarantineDamagedCapture(id, damagedIndex)
                val document = sessions.getById(id) ?: error("저장된 촬영을 찾지 못했습니다.")
                val recovered = recovery.recover(document)
                sessionRevision = recovered.revision
                _uiState.update { it.copy(currentShot = recovered.photos.size,
                    damagedCaptureIndex = null, recoveryBlocked = false, errorMessage = null) }
                if (pauseRequested) _uiState.update { it.copy(phase = CapturePhase.PAUSED) }
                else captureRemaining(id, recovered.captureCount, recovered.photos.size)
            } catch (_: CancellationException) {
                _uiState.update { it.copy(phase = CapturePhase.PAUSED, flash = false) }
            } catch (t: CaptureRecoveryProblem) {
                _uiState.update { it.copy(phase = CapturePhase.FAILED, currentShot = t.completedCount,
                    damagedCaptureIndex = (t as? DamagedPublishedCapture)?.index,
                    recoveryBlocked = t !is DamagedPublishedCapture,
                    errorMessage = t.message, flash = false) }
            } catch (t: Exception) {
                _uiState.update { it.copy(phase = CapturePhase.FAILED,
                    errorMessage = t.message ?: "손상 사진 격리 또는 재촬영에 실패했습니다.", flash = false) }
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
        cameraBindJob?.cancel()
        cameraSwitchJob?.cancel()
        deferredBindOnJob = null
        pendingRebind = false
        boundLifecycleOwner = null
        boundPreviewView = null
        engine.unbind()
    }
}

/** Restores only contiguous, valid captures; never repairs a recorded source by replacing it. */
class CaptureFileRecovery(
    private val storage: FileImageStorage,
    private val sessions: SessionDocumentRepository,
) {
    suspend fun recover(initial: SessionDocument): SessionDocument {
        var current = initial
        val recorded = current.photos.sortedBy { it.captureIndex }
        if (recorded.map { it.captureIndex } != recorded.indices.toList()) {
            throw CaptureSequenceMismatch(current.photos.size)
        }
        recorded.forEach { photo ->
            val file = sessions.resolvePhotoPath(photo)
            if (!storage.isCaptureFileValid(file)) {
                throw RecordedCaptureUnavailable(photo.captureIndex + 1, current.photos.size)
            }
        }
        val files = storage.getCapturePaths(current.sessionId)
        for (filePath in files) {
            val file = java.io.File(filePath)
            val index = file.name.removePrefix("cap_").removeSuffix(".jpg").toIntOrNull()?.minus(1) ?: continue
            if (index !in 0 until current.captureCount || current.photos.any { it.captureIndex == index }) {
                continue
            }
            if (index != current.photos.size) throw CaptureSequenceMismatch(current.photos.size)
            if (!storage.isPublishedCaptureValid(current.sessionId, index + 1)) {
                throw DamagedPublishedCapture(index + 1, current.photos.size)
            }
            current = sessions.appendCapture(
                current.sessionId,
                current.revision,
                PhotoRef(UUID.randomUUID().toString(), "captures/${current.sessionId}/${file.name}", index),
            )
        }
        return current
    }
}
