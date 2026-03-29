package com.pocket4cut.presentation.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocket4cut.data.local.SessionRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.domain.model.PhotoSession
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
    private val sessions = SessionRepository(app.applicationContext)
    private val storage = FileImageStorage(app.applicationContext)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                sessions.getAll()
            }.onSuccess { list ->
                _uiState.update {
                    it.copy(isLoading = false, items = list.map { s -> s.toGalleryItem() })
                }
            }.onFailure { t ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = t.message ?: "불러오기에 실패했습니다.")
                }
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

private fun PhotoSession.toGalleryItem(): GalleryItem {
    val date = Date(createdAt)
    val (kindKey, kindLabel) = frameKindForSession(selectedCount)
    return GalleryItem(
        sessionId = id,
        resultPath = finalImagePath.orEmpty(),
        selectedCount = selectedCount,
        createdAt = createdAt,
        shortDateLabel = shortDateFormat.format(date),
        frameKindKey = kindKey,
        frameKindLabel = kindLabel,
        dayKey = dayKeyFormat.format(date),
        daySectionTitle = daySectionFormat.format(date),
    )
}
