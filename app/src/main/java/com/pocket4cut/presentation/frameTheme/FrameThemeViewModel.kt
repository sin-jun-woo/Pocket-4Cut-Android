package com.pocket4cut.presentation.frameTheme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.data.storage.FileImageStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FrameThemeUiState(
    val imagePaths: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedThemeId: String? = null,
)

class FrameThemeViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = FileImageStorage(app.applicationContext)

    private val _uiState = MutableStateFlow(FrameThemeUiState())
    val uiState: StateFlow<FrameThemeUiState> = _uiState

    fun load(sessionId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { storage.getCapturePaths(sessionId) }
                .onSuccess { paths -> _uiState.update { it.copy(isLoading = false, imagePaths = paths) } }
                .onFailure { t -> _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "불러오기 실패") } }
        }
    }

    fun selectTheme(themeId: String) {
        _uiState.update { it.copy(selectedThemeId = themeId) }
    }
}

