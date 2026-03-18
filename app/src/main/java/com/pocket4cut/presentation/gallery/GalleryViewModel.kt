package com.pocket4cut.presentation.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class GalleryItem(
    val path: String,
    val lastModified: Long,
)

data class GalleryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val items: List<GalleryItem> = emptyList(),
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val resultsDir = File(app.filesDir, "results")

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (!resultsDir.exists()) return@withContext emptyList()
                    resultsDir.listFiles()
                        ?.filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png") }
                        ?.map { GalleryItem(path = it.absolutePath, lastModified = it.lastModified()) }
                        ?.sortedByDescending { it.lastModified }
                        .orEmpty()
                }
            }.onSuccess { items ->
                _uiState.update { it.copy(isLoading = false, items = items) }
            }.onFailure { t ->
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "불러오기 실패") }
            }
        }
    }

    fun delete(path: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { File(path).delete() }
            load()
        }
    }
}

