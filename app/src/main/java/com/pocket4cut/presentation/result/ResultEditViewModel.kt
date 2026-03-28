package com.pocket4cut.presentation.result

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.core.util.BitmapAdjustments
import com.pocket4cut.data.local.SessionRepository
import com.pocket4cut.data.storage.FileImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ResultEditUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val preview: Bitmap? = null,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val rotationSteps90: Int = 0,
)

class ResultEditViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = FileImageStorage(app.applicationContext)
    private val sessions = SessionRepository(app.applicationContext)

    private val _state = MutableStateFlow(ResultEditUiState())
    val state: StateFlow<ResultEditUiState> = _state.asStateFlow()

    private var sourcePath: String? = null
    private var decodedBase: Bitmap? = null

    fun load(resultPath: String) {
        sourcePath = resultPath
        decodedBase?.recycle()
        decodedBase = null
        _state.value = ResultEditUiState(isLoading = true)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(resultPath, opts)
                    val maxSide = maxOf(opts.outWidth, opts.outHeight)
                    val sample = when {
                        maxSide <= 0 -> 1
                        maxSide > 1600 -> maxSide / 1600
                        else -> 1
                    }.coerceAtLeast(1)
                    val loadOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeFile(resultPath, loadOpts)
                        ?: error("이미지를 불러올 수 없습니다.")
                }
            }.onSuccess { bmp ->
                decodedBase = bmp
                val previewBmp = withContext(Dispatchers.IO) {
                    buildPreviewForBase(
                        bmp,
                        ResultEditUiState(
                            isLoading = false,
                            brightness = 0f,
                            contrast = 1f,
                            rotationSteps90 = 0,
                        ),
                    )
                }
                _state.value = ResultEditUiState(
                    isLoading = false,
                    preview = previewBmp,
                    brightness = 0f,
                    contrast = 1f,
                    rotationSteps90 = 0,
                )
            }.onFailure { t ->
                _state.update {
                    it.copy(isLoading = false, errorMessage = t.message ?: "불러오기 실패")
                }
            }
        }
    }

    fun setBrightness(v: Float) {
        _state.update { it.copy(brightness = v.coerceIn(-0.35f, 0.35f)) }
        refreshPreview()
    }

    fun setContrast(v: Float) {
        _state.update { it.copy(contrast = v.coerceIn(0.65f, 1.5f)) }
        refreshPreview()
    }

    fun rotate90() {
        _state.update { it.copy(rotationSteps90 = (it.rotationSteps90 + 1) % 4) }
        refreshPreview()
    }

    private fun refreshPreview() {
        val base = decodedBase ?: return
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) { buildPreviewForBase(base, _state.value) }
            _state.update { prev ->
                prev.preview?.recycle()
                prev.copy(preview = bmp)
            }
        }
    }

    private fun buildPreviewForBase(base: Bitmap, s: ResultEditUiState): Bitmap {
        var step = base
        repeat(s.rotationSteps90 % 4) {
            val next = BitmapAdjustments.rotate90(step)
            if (step !== base) step.recycle()
            step = next
        }
        val needTone = s.brightness != 0f || s.contrast != 1f
        return if (needTone) {
            val out = BitmapAdjustments.applyBrightnessContrast(step, s.brightness, s.contrast)
            if (step !== base) step.recycle()
            out
        } else {
            if (step === base) step.copy(step.config ?: Bitmap.Config.ARGB_8888, false) else step
        }
    }

    suspend fun saveToFile() {
        val path = sourcePath ?: error("경로 없음")
        val s = _state.value
        withContext(Dispatchers.IO) {
            val base = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inScaled = false })
                ?: error("원본 디코딩 실패")
            var step = base
            repeat(s.rotationSteps90 % 4) {
                val r = BitmapAdjustments.rotate90(step)
                if (step !== base) step.recycle()
                step = r
            }
            val out = BitmapAdjustments.applyBrightnessContrast(step, s.brightness, s.contrast)
            if (step !== base) step.recycle()
            base.recycle()
            try {
                storage.saveBitmapToPath(out, path)
            } finally {
                out.recycle()
            }
        }
        sessionIdFromResultPath(path)?.let { sid ->
            sessions.getById(sid)?.let { existing ->
                sessions.upsert(existing.copy(finalImagePath = path))
            }
        }
    }

    override fun onCleared() {
        _state.value.preview?.recycle()
        decodedBase?.recycle()
        decodedBase = null
        super.onCleared()
    }
}

fun sessionIdFromResultPath(path: String): String? {
    val name = File(path).nameWithoutExtension
    return name.removeSuffix("_result").takeIf { name.endsWith("_result") && it.isNotBlank() }
}
