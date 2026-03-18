package com.pocket4cut.presentation.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.data.local.SessionRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.domain.model.PhotoSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GalleryItem(
    val sessionId: String,
    val resultPath: String,
    val createdAt: Long,
)

data class GalleryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val items: List<GalleryItem> = emptyList(),
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val sessions = SessionRepository(app.applicationContext)
    private val storage = FileImageStorage(app.applicationContext)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                sessions.getAll()
            }.onSuccess { items ->
                _uiState.update { it.copy(isLoading = false, items = items.map { it.toItem() }) }
            }.onFailure { t ->
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "불러오기 실패") }
            }
        }
    }

    fun delete(sessionId: String) {
        viewModelScope.launch {
            sessions.delete(sessionId)
            storage.deleteSessionFiles(sessionId)
            load()
        }
    }
}

private fun PhotoSession.toItem(): GalleryItem =
    GalleryItem(
        sessionId = id,
        resultPath = finalImagePath.orEmpty(),
        createdAt = createdAt,
    )

