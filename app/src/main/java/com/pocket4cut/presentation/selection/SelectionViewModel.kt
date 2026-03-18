package com.pocket4cut.presentation.selection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.data.storage.FileImageStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelectionUiState(
    val imagePaths: List<String> = emptyList(),
    val selectedIndexes: List<Int> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class SelectionViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = FileImageStorage(app.applicationContext)

    private val _uiState = MutableStateFlow(SelectionUiState())
    val uiState: StateFlow<SelectionUiState> = _uiState

    fun load(sessionId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, selectedIndexes = emptyList()) }
        viewModelScope.launch {
            runCatching {
                storage.getCapturePaths(sessionId)
            }.onSuccess { paths ->
                _uiState.update { it.copy(isLoading = false, imagePaths = paths) }
            }.onFailure { t ->
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "불러오기 실패") }
            }
        }
    }

    fun toggle(index: Int, max: Int) {
        _uiState.update { state ->
            val current = state.selectedIndexes.toMutableList()
            val existing = current.indexOf(index)
            if (existing >= 0) {
                current.removeAt(existing)
            } else {
                if (current.size < max) current.add(index)
            }
            state.copy(selectedIndexes = current)
        }
    }
}

