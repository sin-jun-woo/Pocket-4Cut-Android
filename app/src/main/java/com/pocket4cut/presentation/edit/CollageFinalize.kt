package com.pocket4cut.presentation.edit

import android.app.Application
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.data.local.SessionRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.domain.model.PhotoSession
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.frame.FrameDefinitions
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CollageFinalize {
    private fun todayString(): String =
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

    suspend fun finalize(
        app: Application,
        sessionId: String,
        frameType: FrameType,
        themeId: String,
        selectedIndexes: List<Int>,
        params: PendingCollageParams,
    ): String = withContext(Dispatchers.IO) {
        val storage = FileImageStorage(app)
        val sessions = SessionRepository(app)
        val allPaths = storage.getCapturePaths(sessionId)
        val picked = selectedIndexes.mapNotNull { idx -> allPaths.getOrNull(idx) }
        val theme = FrameDefinitions.byId(themeId) ?: error("테마를 찾을 수 없습니다.")
        val orderedPaths = params.order.mapNotNull { i -> picked.getOrNull(i) }
        val bitmaps = orderedPaths.mapNotNull { path ->
            BitmapDecoding.decodeSampled(path, reqSize = 1400)
        }
        val dateText = if (params.showDate) todayString() else null
        val result = try {
            CollageRenderer.render(
                frameType = frameType,
                theme = theme,
                bitmaps = bitmaps,
                filter = params.filter,
                text = params.text.takeIf { it.isNotBlank() },
                dateText = dateText,
                targetWidth = 1920,
            )
        } finally {
            bitmaps.forEach { it.recycle() }
        }
        try {
            val path = storage.saveResult(result, sessionId)
            sessions.upsert(
                PhotoSession(
                    id = sessionId,
                    captureCount = frameType.captureCount,
                    selectedCount = frameType.selectCount,
                    imagePaths = picked,
                    selectedIndexes = selectedIndexes,
                    frameId = theme.id,
                    finalImagePath = path,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            path
        } finally {
            result.recycle()
        }
    }
}
