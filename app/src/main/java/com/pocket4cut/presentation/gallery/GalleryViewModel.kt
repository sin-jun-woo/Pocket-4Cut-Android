package com.pocket4cut.presentation.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.data.importing.PhotoImportRepository
import com.pocket4cut.data.local.ResultPublicationIssue
import com.pocket4cut.data.local.SessionCorruptException
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.data.local.SessionStoreException
import com.pocket4cut.domain.model.ResultRecord
import com.pocket4cut.domain.model.InputSource
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionStage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GalleryItem(
    val sessionId: String,
    val resultPath: String,
    val selectedCount: Int,
    val createdAt: Long,
    val shortDateLabel: String,
    /** iOS `byKind` 그룹 — "2", "4", "6" */
    val frameKindKey: String,
    val frameKindLabel: String,
    /** iOS `byDate` 일 단위 섹션 */
    val dayKey: String,
    val daySectionTitle: String,
    val resultId: String = "",
    val missingImage: Boolean = false,
)

data class GalleryDraftItem(
    val sessionId: String,
    val stage: SessionStage,
    val selectedCount: Int,
    val completedShots: Int,
    val totalShots: Int,
    val updatedAt: Long,
    val thumbnailPath: String?,
    val inputSource: InputSource? = null,
)

data class GalleryBucket(
    val key: String,
    val label: String,
    val items: List<GalleryItem>,
)

data class GalleryRecoveryState(
    val sessionId: String,
    val isLoading: Boolean = true,
    val documentNeedsRecovery: Boolean = false,
    val canHideUnreadable: Boolean = false,
    val publicationIssues: List<ResultPublicationIssue> = emptyList(),
    val message: String? = null,
)

data class GalleryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val items: List<GalleryItem> = emptyList(),
    val drafts: List<GalleryDraftItem> = emptyList(),
    val unreadableSessionIds: Set<String> = emptySet(),
    val legacyMigrationFailed: Boolean = false,
    val hiddenSessionIds: List<String> = emptyList(),
    val recovery: GalleryRecoveryState? = null,
) {
    /** 종류별: 2컷 → 4컷 → 6컷 */
    val kindBuckets: List<GalleryBucket>
        get() = items
            .groupBy { it.frameKindKey }
            .entries
            .sortedBy { (k, _) ->
                when (k) {
                    "2" -> 0
                    "4" -> 1
                    "6" -> 2
                    else -> 3
                }
            }
            .map { (_, bucketItems) ->
                GalleryBucket(
                    key = bucketItems.first().frameKindKey,
                    label = bucketItems.first().frameKindLabel,
                    items = bucketItems.sortedByDescending { it.createdAt },
                )
            }

    /** 날짜별(일): 최신 날짜 우선 */
    val dayBuckets: List<GalleryBucket>
        get() = items
            .groupBy { it.dayKey }
            .entries
            .sortedByDescending { it.key }
            .map { (_, bucketItems) ->
                GalleryBucket(
                    key = bucketItems.first().dayKey,
                    label = bucketItems.first().daySectionTitle,
                    items = bucketItems.sortedByDescending { it.createdAt },
                )
            }
}

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val sessions = SessionDocumentRepository(app.applicationContext)
    private val imports = PhotoImportRepository(app.applicationContext, sessions)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val importRecoveries = imports.recoverAll()
                val scan = sessions.scanForGallery()
                val hidden = sessions.listHiddenSessionIds()
                val list = scan.documents
                val items = list.flatMap { document ->
                    document.results.map { result -> document.toGalleryItem(result, sessions) }
                }
                val drafts = list.filter { it.stage != SessionStage.RESULT }.map { document ->
                    GalleryDraftItem(
                        sessionId = document.sessionId,
                        stage = document.stage,
                        selectedCount = document.selectedCount,
                        completedShots = document.photos.size,
                        totalShots = document.captureCount,
                        updatedAt = document.updatedAt,
                        thumbnailPath = document.photos.lastOrNull()?.let { photo ->
                            sessions.resolvePhotoPath(photo).absolutePath
                        },
                        inputSource = document.inputSource,
                    )
                }.plus(scan.unreadableSessionIds.map { sessionId ->
                    GalleryDraftItem(
                        sessionId = sessionId,
                        stage = SessionStage.NEEDS_RECOVERY,
                        selectedCount = 0,
                        completedShots = 0,
                        totalShots = 0,
                        updatedAt = 0,
                        thumbnailPath = null,
                    )
                }).sortedByDescending { it.updatedAt }
                _uiState.update {
                    it.copy(isLoading = false, items = items, drafts = drafts,
                        unreadableSessionIds = scan.unreadableSessionIds.toSet(),
                        legacyMigrationFailed = scan.legacyMigrationError != null,
                        hiddenSessionIds = hidden,
                        errorMessage = if (importRecoveries.any {
                                recovery -> recovery.failures.isNotEmpty() || recovery.needsReselection
                            }
                        ) {
                            "일부 앨범 가져오기를 복구하지 못했습니다. 다시 선택이 필요한 사진을 확인해 주세요."
                        } else null)
                }
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = t.message ?: "불러오기에 실패했습니다.")
                }
            }
        }
    }

    fun delete(sessionId: String) {
        viewModelScope.launch {
            try {
                sessions.requestDelete(sessionId)
                load()
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                _uiState.update { it.copy(errorMessage = t.message ?: "삭제에 실패했습니다.") }
            }
        }
    }

    fun discardDraft(sessionId: String) {
        viewModelScope.launch {
            try {
                val current = sessions.getById(sessionId) ?: return@launch
                sessions.discardDraft(sessionId, current.revision)
                load()
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                _uiState.update { it.copy(errorMessage = t.message ?: "작업을 버리지 못했습니다.") }
            }
        }
    }

    fun reportMissingImage() {
        _uiState.update { it.copy(errorMessage = "결과 이미지 파일을 찾지 못했습니다. 작업을 삭제하거나 다시 시도해 주세요.") }
    }

    fun inspectRecovery(sessionId: String) {
        _uiState.update { it.copy(recovery = GalleryRecoveryState(sessionId)) }
        viewModelScope.launch {
            try {
                val issues = sessions.listResultPublicationIssues(sessionId)
                _uiState.update { state ->
                    state.copy(recovery = GalleryRecoveryState(
                        sessionId = sessionId,
                        isLoading = false,
                        publicationIssues = issues,
                        message = if (issues.isEmpty()) "미완료 결과 기록을 찾지 못했습니다. 다시 불러와 주세요." else null,
                    ))
                }
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                _uiState.update { state ->
                    state.copy(recovery = GalleryRecoveryState(
                        sessionId = sessionId,
                        isLoading = false,
                        documentNeedsRecovery = t is SessionCorruptException,
                        canHideUnreadable = sessionId in state.unreadableSessionIds,
                        message = if (t is SessionCorruptException) {
                            "세션 기록 자체에 복구가 필요합니다. 이전 정상본 복구를 시도할 수 있습니다. 촬영 원본과 기존 완료본은 그대로 보존됩니다."
                        } else if (t is SessionStoreException) {
                            "세션 형식을 읽지 못했습니다. 현재 앱이 지원하지 않는 형식이거나 기록이 손상됐을 수 있습니다. 파일은 보존됩니다."
                        } else t.message ?: "복구 상태를 확인하지 못했습니다.",
                    ))
                }
            }
        }
    }

    fun recoverDocument(sessionId: String) {
        _uiState.update { state -> state.copy(recovery = state.recovery?.copy(isLoading = true)) }
        viewModelScope.launch {
            try {
                val recovered = sessions.recover(sessionId)
                if (recovered == null || recovered.stage == SessionStage.NEEDS_RECOVERY) {
                    _uiState.update { state -> state.copy(recovery = state.recovery?.copy(
                        isLoading = false,
                        message = "자동 복구 가능한 이전 정상본이 없습니다. 촬영 원본과 기존 완료본은 보존되어 있습니다.",
                    )) }
                } else {
                    _uiState.update { it.copy(recovery = null) }
                    load()
                }
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                _uiState.update { state -> state.copy(recovery = state.recovery?.copy(
                    isLoading = false,
                    message = t.message ?: "세션 기록 복구에 실패했습니다.",
                )) }
            }
        }
    }

    fun quarantinePublication(sessionId: String, resultId: String) {
        _uiState.update { state -> state.copy(recovery = state.recovery?.copy(isLoading = true)) }
        viewModelScope.launch {
            try {
                sessions.quarantineResultPublication(sessionId, resultId)
                _uiState.update { it.copy(recovery = null) }
                load()
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                _uiState.update { state -> state.copy(recovery = state.recovery?.copy(
                    isLoading = false,
                    message = t.message ?: "미완료 결과 격리에 실패했습니다.",
                )) }
            }
        }
    }

    fun dismissRecovery() {
        _uiState.update { it.copy(recovery = null) }
    }

    fun hideUnreadableSession(sessionId: String) {
        _uiState.update { state -> state.copy(recovery = state.recovery?.copy(isLoading = true)) }
        viewModelScope.launch {
            try {
                sessions.hideUnreadableSession(sessionId)
                _uiState.update { it.copy(recovery = null) }
                load()
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                _uiState.update { state -> state.copy(recovery = state.recovery?.copy(
                    isLoading = false,
                    message = t.message ?: "손상 기록을 숨기지 못했습니다.",
                )) }
            }
        }
    }

    fun unhideSession(sessionId: String) {
        viewModelScope.launch {
            try {
                sessions.unhideSession(sessionId)
                load()
            } catch (t: Exception) {
                if (t is CancellationException) throw t
                _uiState.update { it.copy(errorMessage = t.message ?: "숨긴 기록을 다시 표시하지 못했습니다.") }
            }
        }
    }
}

private val shortDateFormat = SimpleDateFormat("M.d", Locale.KOREA)
private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
private val daySectionFormat = SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREA)

private fun frameKindForSession(selectedCount: Int): Pair<String, String> =
    when (selectedCount) {
        2 -> "2" to "2컷"
        4 -> "4" to "4컷"
        6 -> "6" to "6컷"
        else -> "x_$selectedCount" to "${selectedCount}컷"
    }

private fun SessionDocument.toGalleryItem(
    result: ResultRecord,
    repository: SessionDocumentRepository,
): GalleryItem {
    val date = Date(result.createdAt)
    val (kindKey, kindLabel) = frameKindForSession(selectedCount)
    val resolved = repository.resolveResultPath(result)
    return GalleryItem(
        sessionId = sessionId,
        resultPath = resolved.absolutePath,
        selectedCount = selectedCount,
        createdAt = result.createdAt,
        shortDateLabel = shortDateFormat.format(date),
        frameKindKey = kindKey,
        frameKindLabel = kindLabel,
        dayKey = dayKeyFormat.format(date),
        daySectionTitle = daySectionFormat.format(date),
        resultId = result.resultId,
        missingImage = !resolved.isFile,
    )
}
