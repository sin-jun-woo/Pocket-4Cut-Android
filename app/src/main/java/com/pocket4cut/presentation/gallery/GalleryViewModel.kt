package com.pocket4cut.presentation.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.ResultRecord
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
)

data class GalleryBucket(
    val key: String,
    val label: String,
    val items: List<GalleryItem>,
)

data class GalleryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val items: List<GalleryItem> = emptyList(),
    val drafts: List<GalleryDraftItem> = emptyList(),
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

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val list = sessions.list()
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
                    )
                }.sortedByDescending { it.updatedAt }
                _uiState.update {
                    it.copy(isLoading = false, items = items, drafts = drafts)
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

    fun reportRecoveryRequired() {
        _uiState.update { it.copy(errorMessage = "이전 작업의 사진 순서 또는 파일을 확정할 수 없습니다. 원본을 보존했습니다. 이 작업은 자동으로 이어갈 수 없습니다.") }
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
