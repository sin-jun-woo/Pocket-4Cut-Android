package com.pocket4cut.presentation.selection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.SessionStage
import kotlinx.coroutines.CancellationException
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
    val hasLoaded: Boolean = false,
    val isCompleting: Boolean = false,
    val errorMessage: String? = null,
)

class SelectionViewModel internal constructor(
    app: Application,
    private val beforeSelectionWrite: suspend () -> Unit,
) : AndroidViewModel(app) {
    constructor(app: Application) : this(app, {})

    private val sessions = SessionDocumentRepository(app.applicationContext)
    private var currentSessionId: String? = null
    private var persistedSelection: List<Int>? = null
    private val writeMutex = Mutex()
    private var completionInFlight = false

    private val _uiState = MutableStateFlow(SelectionUiState())
    val uiState: StateFlow<SelectionUiState> = _uiState

    fun load(sessionId: String, maxSelection: Int) {
        // Rotation must not replace unsaved choices; clean re-entry still reads later edits.
        val state = _uiState.value
        if (completionInFlight) return
        if (currentSessionId == sessionId &&
            (state.isLoading || (state.hasLoaded && state.selectedIndexes != persistedSelection))
        ) return
        currentSessionId = sessionId
        persistedSelection = null
        _uiState.value = SelectionUiState(
            isLoading = true,
            maxSelection = maxSelection,
        )
        viewModelScope.launch {
            runCatching {
                val document = sessions.getById(sessionId) ?: error("촬영 기록을 찾지 못했습니다.")
                check(document.stage != SessionStage.NEEDS_RECOVERY) {
                    "저장 기록 복구가 필요합니다. 작업 보관함에서 확인해 주세요."
                }
                val ordered = document.photos.sortedBy { it.captureIndex }
                val paths = ordered.map { ref ->
                    sessions.resolvePhotoPath(ref).also { check(it.isFile) { "사진 파일이 없습니다." } }.absolutePath
                }
                val selected = document.draft.selectedPhotoIdsInOrder.map { id ->
                    ordered.indexOfFirst { it.photoId == id }.also { check(it >= 0) { "선택한 사진이 없습니다." } }
                }
                paths to selected
            }.onSuccess { (paths, selected) ->
                persistedSelection = selected
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasLoaded = true,
                        imagePaths = paths,
                        selectedIndexes = selected,
                        errorMessage = null,
                    )
                }
            }.onFailure { t ->
                if (t is CancellationException) throw t
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "불러오기에 실패했습니다.") }
            }
        }
    }

    fun toggle(index: Int, max: Int) {
        if (!_uiState.value.hasLoaded || _uiState.value.isCompleting ||
            index !in _uiState.value.imagePaths.indices
        ) return
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
        retrySave()
    }

    fun retrySave() {
        if (!_uiState.value.hasLoaded || _uiState.value.isCompleting) return
        val id = currentSessionId ?: return
        val selected = _uiState.value.selectedIndexes
        viewModelScope.launch {
            runCatching {
                writeMutex.withLock {
                    persistSelection(id, selected, SessionStage.SELECT)
                }
            }.onFailure { cause ->
                if (cause is CancellationException) throw cause
                _uiState.update { it.copy(errorMessage = cause.message ?: "선택을 저장하지 못했습니다.") }
            }
        }
    }

    fun complete(onSaved: (List<Int>) -> Unit) {
        if (!_uiState.value.hasLoaded || _uiState.value.isCompleting) return
        val id = currentSessionId ?: return
        val selected = _uiState.value.selectedIndexes
        if (selected.size != _uiState.value.maxSelection) return
        // Lock synchronously, before the coroutine can suspend behind an earlier autosave.
        completionInFlight = true
        _uiState.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            try {
                writeMutex.withLock {
                    persistSelection(id, selected, SessionStage.FRAME)
                }
                // Keep the outgoing screen locked. A later load on re-entry resets the state.
                onSaved(selected)
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                _uiState.update {
                    it.copy(isCompleting = false, errorMessage = cause.message ?: "선택을 저장하지 못했습니다.")
                }
            } finally {
                completionInFlight = false
            }
        }
    }

    fun leave(onSaved: () -> Unit, onFailed: () -> Unit) {
        if (_uiState.value.isCompleting) return
        // A failed initial load has no editable state to write. In particular, an empty
        // UI selection must never overwrite the stored order when a source file is missing.
        if (!_uiState.value.hasLoaded) {
            onSaved()
            return
        }
        val id = currentSessionId ?: run { onSaved(); return }
        val selected = _uiState.value.selectedIndexes
        viewModelScope.launch {
            runCatching { writeMutex.withLock { persistSelection(id, selected, SessionStage.SELECT) } }
                .onSuccess { onSaved() }
                .onFailure { cause ->
                    if (cause is CancellationException) throw cause
                    _uiState.update { it.copy(errorMessage = cause.message ?: "선택을 저장하지 못했습니다.") }
                    onFailed()
                }
        }
    }

    private suspend fun persistSelection(id: String, selected: List<Int>, stage: SessionStage) {
        beforeSelectionWrite()
        val doc = sessions.getById(id) ?: error("촬영 기록을 찾지 못했습니다.")
        val ordered = doc.photos.sortedBy { it.captureIndex }
        val ids = selected.map { ordered[it].photoId }
        sessions.update(id, doc.revision) {
            it.copy(stage = stage, draft = it.draft.copy(selectedPhotoIdsInOrder = ids))
        }
        if (currentSessionId == id) persistedSelection = selected
        _uiState.update { state ->
            if (currentSessionId == id && state.selectedIndexes == selected) state.copy(errorMessage = null)
            else state
        }
    }

    fun selectionOrder(index: Int): Int? {
        val pos = _uiState.value.selectedIndexes.indexOf(index)
        return if (pos >= 0) pos + 1 else null
    }

    val canComplete: Boolean
        get() {
            val s = _uiState.value
            return s.hasLoaded && !s.isCompleting && s.maxSelection > 0 && s.selectedIndexes.size == s.maxSelection
        }
}

