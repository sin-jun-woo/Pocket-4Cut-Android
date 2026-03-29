package com.pocket4cut.presentation.capture

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.pocket4cut.camera.CaptureEngine
import com.pocket4cut.core.util.Constants
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.presentation.settings.AppSettings
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.Job
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
    COUNTDOWN,
    CAPTURING,
    COMPLETED,
    FAILED,
}

class CaptureViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = CaptureEngine(app.applicationContext)
    private val storage = FileImageStorage(app.applicationContext)

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState

    private var captureJob: Job? = null
    private var boundLifecycleOwner: LifecycleOwner? = null
    private var boundPreviewView: PreviewView? = null

    /** 카운트다운 중 수동 셔터 (남은 초는 버리고 즉시 촬영) */
    private val manualShutter = Channel<Unit>(Channel.CONFLATED)

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
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
                val range = engine.zoomRatioRange() ?: (1f to 1f)
                val z = engine.currentZoomRatio()?.coerceIn(range.first, range.second) ?: range.first
                _uiState.update {
                    it.copy(
                        phase = CapturePhase.READY,
                        errorMessage = null,
                        minZoom = range.first,
                        maxZoom = range.second,
                        zoomRatio = z,
                    )
                }
            }.onFailure { t ->
                _uiState.update { it.copy(phase = CapturePhase.FAILED, errorMessage = t.message ?: "카메라 연결에 실패했습니다.") }
            }
        }
    }

    fun switchCamera() {
        if (captureJob?.isActive == true) return
        val lifecycleOwner = boundLifecycleOwner ?: return
        val previewView = boundPreviewView ?: return
        val previous = _uiState.value.isFrontCamera
        _uiState.update { it.copy(isFrontCamera = !previous) }
        viewModelScope.launch {
            runCatching {
                val lensFacing = if (_uiState.value.isFrontCamera) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
                engine.bind(lifecycleOwner, previewView, lensFacing)
                val range = engine.zoomRatioRange() ?: (1f to 1f)
                val z = engine.currentZoomRatio()?.coerceIn(range.first, range.second) ?: range.first
                _uiState.update {
                    it.copy(
                        phase = CapturePhase.READY,
                        errorMessage = null,
                        minZoom = range.first,
                        maxZoom = range.second,
                        zoomRatio = z,
                    )
                }
            }.onFailure { t ->
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

        val sessionId = UUID.randomUUID().toString()
        val total = frameType.captureCount

        drainManualShutter()

        _uiState.update {
            it.copy(
                phase = CapturePhase.READY,
                countdownRemaining = AppSettings.countdownSeconds,
                currentShot = 0,
                totalShots = total,
                sessionId = sessionId,
                flash = false,
                errorMessage = null,
            )
        }

        captureJob = viewModelScope.launch {
            try {
                for (i in 1..total) {
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

                    _uiState.update { it.copy(phase = CapturePhase.CAPTURING, currentShot = i, flash = true) }
                    val file = storage.createCaptureFile(sessionId, i)
                    engine.takePictureToFile(file)
                    delay(300)
                    _uiState.update { it.copy(flash = false) }

                    if (i != total) {
                        delay(Constants.CAPTURE_INTERVAL_SECONDS * 1000L)
                    }
                }

                _uiState.update { it.copy(phase = CapturePhase.COMPLETED, flash = false) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(phase = CapturePhase.FAILED, errorMessage = t.message ?: "촬영에 실패했습니다.", flash = false) }
            }
        }
    }

    fun stop() {
        captureJob?.cancel()
        captureJob = null
        drainManualShutter()
        _uiState.update { it.copy(phase = CapturePhase.IDLE, flash = false, errorMessage = null, currentShot = 0, totalShots = 0) }
    }

    private fun drainManualShutter() {
        while (manualShutter.tryReceive().isSuccess) { }
    }

    override fun onCleared() {
        super.onCleared()
        engine.unbind()
    }
}
