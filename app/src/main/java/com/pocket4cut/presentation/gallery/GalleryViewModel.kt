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
    val monthKey: String,
    val monthLabel: String,
)

data class MonthBucket(
    val key: String,
    val label: String,
    val items: List<GalleryItem>,
)

data class GalleryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val items: List<GalleryItem> = emptyList(),
) {
    val monthBuckets: List<MonthBucket>
        get() = items
            .groupBy { it.monthKey }
            .entries
            .sortedByDescending { it.key }
            .map { (key, bucketItems) ->
                MonthBucket(
                    key = key,
                    label = bucketItems.first().monthLabel,
                    items = bucketItems,
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
private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.KOREA)
private val monthLabelFormat = SimpleDateFormat("yyyy년 M월", Locale.KOREA)

private fun PhotoSession.toGalleryItem(): GalleryItem {
    val date = Date(createdAt)
    return GalleryItem(
        sessionId = id,
        resultPath = finalImagePath.orEmpty(),
        selectedCount = selectedCount,
        createdAt = createdAt,
        shortDateLabel = shortDateFormat.format(date),
        monthKey = monthKeyFormat.format(date),
        monthLabel = monthLabelFormat.format(date),
    )
}
