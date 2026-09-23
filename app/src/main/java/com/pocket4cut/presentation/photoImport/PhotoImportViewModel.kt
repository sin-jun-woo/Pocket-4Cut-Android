package com.pocket4cut.presentation.photoImport

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pocket4cut.data.importing.PhotoImportFailure
import com.pocket4cut.data.importing.PhotoImportFailureReason
import com.pocket4cut.data.importing.PhotoImportRepository
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.InputSource
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun List<PhotoImportFailure>.hasBlockingRecoveryFailure(): Boolean =
    any { failure ->
        failure.reason == PhotoImportFailureReason.STORAGE ||
            failure.reason == PhotoImportFailureReason.CONFLICT
    }

data class ImportedPhotoItem(
    val photoId: String,
    val path: String,
)

data class PhotoImportUiState(
    val isInitialized: Boolean = false,
    val isBusy: Boolean = false,
    val photos: List<ImportedPhotoItem> = emptyList(),
    val message: String? = null,
    val isMessageError: Boolean = false,
    val hasBlockingRecoveryError: Boolean = false,
    val hasUnacknowledgedEditFailure: Boolean = false,
) {
    fun canContinue(requiredCount: Int): Boolean =
        !isBusy &&
            !hasBlockingRecoveryError &&
            !hasUnacknowledgedEditFailure &&
            photos.size == requiredCount

    internal fun dismissMessage(): PhotoImportUiState = if (hasBlockingRecoveryError) {
        this
    } else {
        copy(
            message = null,
            isMessageError = false,
            hasUnacknowledgedEditFailure = false,
        )
    }
}

class PhotoImportViewModel(
    app: Application,
    val frameType: FrameType,
    val sessionId: String,
) : AndroidViewModel(app) {
    private val sessions = SessionDocumentRepository(app.applicationContext)
    private val imports = PhotoImportRepository(app.applicationContext)
    private val _uiState = MutableStateFlow(PhotoImportUiState())
    val uiState: StateFlow<PhotoImportUiState> = _uiState

    init {
        viewModelScope.launch {
            try {
                val recovery = imports.recover(sessionId)
                val document = recovery.session ?: sessions.getById(sessionId)
                if (document != null) {
                    require(document.inputSource == InputSource.ALBUM && document.frameTypeId == frameType.id) {
                        "앨범 작업 정보가 현재 사진 구성과 일치하지 않습니다."
                    }
                }
                _uiState.value = PhotoImportUiState(
                    isInitialized = true,
                    photos = document?.toUiPhotos().orEmpty(),
                    message = buildList {
                        if (recovery.needsReselection) add("접근 권한이 만료된 사진은 다시 선택해 주세요.")
                        recovery.failures.forEach { add(it.message) }
                    }.takeIf { it.isNotEmpty() }?.distinct()?.joinToString("\n"),
                    isMessageError = recovery.failures.isNotEmpty() || recovery.needsReselection,
                    hasBlockingRecoveryError = recovery.failures.hasBlockingRecoveryFailure(),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _uiState.value = PhotoImportUiState(
                    isInitialized = true,
                    message = failure.message ?: "앨범 작업을 복구하지 못했습니다.",
                    isMessageError = true,
                    hasBlockingRecoveryError = true,
                )
            }
        }
    }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val state = _uiState.value
        if (state.isBusy) return
        when (val decision = PhotoSelectionPolicy.evaluate(
            targetCount = frameType.selectCount,
            selectedUris = uris.map(Uri::toString),
        )) {
            is PhotoSelectionDecision.RejectedTooMany -> {
                _uiState.update {
                    it.copy(
                        message = "한 번에 선택 가능한 ${decision.availableCount}장보다 많은 " +
                            "${decision.receivedCount}장이 선택되어 " +
                            "가져오지 않았습니다. 다시 선택해 주세요.",
                        isMessageError = true,
                    )
                }
            }
            is PhotoSelectionDecision.Accepted -> viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, message = null) }
                try {
                    val result = imports.importUris(
                        sessionId = sessionId,
                        frameTypeId = frameType.id,
                        uris = decision.uris.map(Uri::parse),
                    )
                    val document = result.session ?: sessions.getById(sessionId)
                    val duplicateCount = decision.duplicateUris.size + result.duplicates.size
                    val notices = buildList {
                        if (duplicateCount > 0) add("같은 사진 ${duplicateCount}장은 중복으로 추가하지 않았습니다.")
                        if (result.failures.isNotEmpty()) {
                            add(result.failures.joinToString(separator = "\n") { it.message })
                        }
                    }
                    _uiState.value = PhotoImportUiState(
                        isInitialized = true,
                        photos = document?.toUiPhotos().orEmpty(),
                        message = notices.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                        isMessageError = result.failures.isNotEmpty(),
                        hasBlockingRecoveryError = result.failures.hasBlockingRecoveryFailure(),
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            message = failure.message ?: "사진을 가져오지 못했습니다. 다시 선택해 주세요.",
                            isMessageError = true,
                            hasBlockingRecoveryError = true,
                        )
                    }
                }
            }
        }
    }

    fun movePhoto(fromIndex: Int, toIndex: Int) {
        val photos = _uiState.value.photos
        if (_uiState.value.isBusy || fromIndex !in photos.indices || toIndex !in photos.indices || fromIndex == toIndex) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            try {
                val document = sessions.getById(sessionId) ?: error("가져온 사진 작업을 찾지 못했습니다.")
                val reordered = document.draft.selectedPhotoIdsInOrder.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
                val updated = sessions.update(sessionId, document.revision) { current ->
                    if (current.inputSource != InputSource.ALBUM || current.results.isNotEmpty()) {
                        error("완료된 작업의 사진 순서는 여기에서 변경할 수 없습니다.")
                    }
                    current.copy(
                        stage = SessionStage.IMPORT,
                        draft = current.draft.copy(selectedPhotoIdsInOrder = reordered),
                    )
                }
                _uiState.value = PhotoImportUiState(
                    isInitialized = true,
                    photos = updated.toUiPhotos(),
                    hasBlockingRecoveryError = _uiState.value.hasBlockingRecoveryError,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                reloadAfterFailure(failure, "사진 순서를 저장하지 못했습니다.")
            }
        }
    }

    fun removePhoto(photoId: String) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            try {
                val document = sessions.getById(sessionId) ?: error("가져온 사진 작업을 찾지 못했습니다.")
                val updated = imports.removePhoto(sessionId, photoId, document.revision)
                _uiState.value = PhotoImportUiState(
                    isInitialized = true,
                    photos = updated.toUiPhotos(),
                    hasBlockingRecoveryError = _uiState.value.hasBlockingRecoveryError,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                reloadAfterFailure(failure, "사진을 제거하지 못했습니다.")
            }
        }
    }

    fun clearMessage() {
        _uiState.update(PhotoImportUiState::dismissMessage)
    }

    private suspend fun reloadAfterFailure(failure: Exception, fallback: String) {
        val current = runCatching { sessions.getById(sessionId) }.getOrNull()
        _uiState.value = PhotoImportUiState(
            isInitialized = true,
            photos = current?.toUiPhotos().orEmpty(),
            message = failure.message ?: fallback,
            isMessageError = true,
            hasBlockingRecoveryError = _uiState.value.hasBlockingRecoveryError,
            // The list above was reloaded from the persisted document, so it is safe to
            // continue only after the user acknowledges that their requested edit failed.
            hasUnacknowledgedEditFailure = true,
        )
    }

    private fun SessionDocument.toUiPhotos(): List<ImportedPhotoItem> {
        val byId = photos.associateBy { it.photoId }
        return draft.selectedPhotoIdsInOrder.mapNotNull { id ->
            byId[id]?.let { ImportedPhotoItem(id, sessions.resolvePhotoPath(it).absolutePath) }
        }
    }

    class Factory(
        private val app: Application,
        private val frameType: FrameType,
        private val sessionId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PhotoImportViewModel(app, frameType, sessionId) as T
    }
}
