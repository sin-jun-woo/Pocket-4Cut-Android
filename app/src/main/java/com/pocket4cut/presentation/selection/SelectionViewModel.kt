package com.pocket4cut.presentation.selection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.SessionStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SelectionUiState(
    val imagePaths: List<String> = emptyList(),
    val selectedIndexes: List<Int> = emptyList(),
    val maxSelection: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class SelectionViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = FileImageStorage(app.applicationContext)
    private val sessions = SessionDocumentRepository(app.applicationContext)
    private var currentSessionId: String? = null
    private val writeMutex = Mutex()

    private val _uiState = MutableStateFlow(SelectionUiState())
    val uiState: StateFlow<SelectionUiState> = _uiState

    fun load(sessionId: String, maxSelection: Int) {
        currentSessionId = sessionId
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                maxSelection = maxSelection,
            )
        }
        viewModelScope.launch {
            runCatching {
                val document = sessions.getById(sessionId) ?: error("촬영 기록을 찾지 못했습니다.")
                val ordered = document.photos.sortedBy { it.captureIndex }
                val paths = ordered.map { ref ->
                    sessions.resolvePhotoPath(ref).also { check(it.isFile) { "사진 파일이 없습니다." } }.absolutePath
                }
                val selected = document.draft.selectedPhotoIdsInOrder.map { id ->
                    ordered.indexOfFirst { it.photoId == id }.also { check(it >= 0) { "선택한 사진이 없습니다." } }
                }
                paths to selected
            }.onSuccess { (paths, selected) ->
                _uiState.update {
                    it.copy(isLoading = false, imagePaths = paths, selectedIndexes = selected)
                }
            }.onFailure { t ->
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "불러오기에 실패했습니다.") }
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
        val id = currentSessionId ?: return
        val selected = _uiState.value.selectedIndexes
        viewModelScope.launch {
            runCatching {
                writeMutex.withLock {
                    persistSelection(id, selected, SessionStage.SELECT)
                }
            }.onFailure { cause ->
                _uiState.update { it.copy(errorMessage = cause.message ?: "선택을 저장하지 못했습니다.") }
            }
        }
    }

    fun complete(onSaved: (List<Int>) -> Unit) {
        val id = currentSessionId ?: return
        val selected = _uiState.value.selectedIndexes
        if (selected.size != _uiState.value.maxSelection) return
        viewModelScope.launch {
            runCatching {
                writeMutex.withLock {
                    persistSelection(id, selected, SessionStage.FRAME)
                }
            }.onSuccess { onSaved(selected) }
                .onFailure { cause ->
                    _uiState.update { it.copy(errorMessage = cause.message ?: "선택을 저장하지 못했습니다.") }
                }
        }
    }

    fun leave(onSaved: () -> Unit, onFailed: () -> Unit) {
        val id = currentSessionId ?: run { onSaved(); return }
        val selected = _uiState.value.selectedIndexes
        viewModelScope.launch {
            runCatching { writeMutex.withLock { persistSelection(id, selected, SessionStage.SELECT) } }
                .onSuccess { onSaved() }
                .onFailure { cause ->
                    _uiState.update { it.copy(errorMessage = cause.message ?: "선택을 저장하지 못했습니다.") }
                    onFailed()
                }
        }
    }

    private suspend fun persistSelection(id: String, selected: List<Int>, stage: SessionStage) {
        val doc = sessions.getById(id) ?: error("촬영 기록을 찾지 못했습니다.")
        val ordered = doc.photos.sortedBy { it.captureIndex }
        val ids = selected.map { ordered[it].photoId }
        sessions.update(id, doc.revision) {
            it.copy(stage = stage, draft = it.draft.copy(selectedPhotoIdsInOrder = ids))
        }
    }

    fun selectionOrder(index: Int): Int? {
        val pos = _uiState.value.selectedIndexes.indexOf(index)
        return if (pos >= 0) pos + 1 else null
    }

    val canComplete: Boolean
        get() {
            val s = _uiState.value
            return s.maxSelection > 0 && s.selectedIndexes.size == s.maxSelection
        }
}

