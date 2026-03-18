package com.pocket4cut.presentation.capture

import android.app.Application
import android.media.MediaActionSound
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.pocket4cut.camera.CaptureEngine
import com.pocket4cut.core.util.Constants
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CaptureUiState(
    val phase: CapturePhase = CapturePhase.IDLE,
    val countdownRemaining: Int = Constants.COUNTDOWN_SECONDS,
    val currentShot: Int = 0,
    val totalShots: Int = 0,
    val sessionId: String? = null,
    val flash: Boolean = false,
    val errorMessage: String? = null,
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
    private val actionSound = MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState

    private var captureJob: Job? = null

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            runCatching {
                engine.bind(lifecycleOwner, previewView)
                _uiState.update { it.copy(phase = CapturePhase.READY, errorMessage = null) }
            }.onFailure { t ->
                _uiState.update { it.copy(phase = CapturePhase.FAILED, errorMessage = t.message ?: "카메라 바인딩 실패") }
            }
        }
    }

    fun start(frameType: FrameType) {
        if (captureJob?.isActive == true) return

        val sessionId = UUID.randomUUID().toString()
        val total = frameType.captureCount

        _uiState.update {
            it.copy(
                phase = CapturePhase.READY,
                countdownRemaining = Constants.COUNTDOWN_SECONDS,
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
                    for (sec in Constants.COUNTDOWN_SECONDS downTo 1) {
                        _uiState.update {
                            it.copy(
                                phase = CapturePhase.COUNTDOWN,
                                countdownRemaining = sec,
                                currentShot = i - 1,
                                totalShots = total,
                                flash = false,
                            )
                        }
                        delay(1000)
                    }

                    _uiState.update { it.copy(phase = CapturePhase.CAPTURING, currentShot = i, flash = true) }
                    val file = storage.createCaptureFile(sessionId, i)
                    engine.takePictureToFile(file)
                    actionSound.play(MediaActionSound.SHUTTER_CLICK)
                    delay(300)
                    _uiState.update { it.copy(flash = false) }

                    if (i != total) {
                        delay(Constants.CAPTURE_INTERVAL_SECONDS * 1000L)
                    }
                }

                _uiState.update { it.copy(phase = CapturePhase.COMPLETED, flash = false) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(phase = CapturePhase.FAILED, errorMessage = t.message ?: "촬영 실패", flash = false) }
            }
        }
    }

    fun stop() {
        captureJob?.cancel()
        captureJob = null
        _uiState.update { it.copy(phase = CapturePhase.IDLE, flash = false, errorMessage = null, currentShot = 0, totalShots = 0) }
    }

    override fun onCleared() {
        super.onCleared()
        engine.unbind()
        actionSound.release()
    }
}

