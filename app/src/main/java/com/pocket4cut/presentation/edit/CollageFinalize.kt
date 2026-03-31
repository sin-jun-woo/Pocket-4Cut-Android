package com.pocket4cut.presentation.edit

import android.app.Application
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.core.util.Constants
import com.pocket4cut.data.local.SessionRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.domain.model.PhotoSession
import com.pocket4cut.frame.*
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
        frameLayoutId: FrameLayoutId,
        selectedIndexes: List<Int>,
        params: PendingCollageParams,
    ): String = withContext(Dispatchers.IO) {
        val storage = FileImageStorage(app)
        val sessions = SessionRepository(app)
        val allPaths = storage.getCapturePaths(sessionId)
        val picked = selectedIndexes.mapNotNull { idx -> allPaths.getOrNull(idx) }
        val style = FrameLayouts.byId(frameLayoutId)
        val frameColor = FrameColors.byId(params.frameColorId)
        val theme = FrameCatalog.themes(frameType).firstOrNull { it.id == params.themeId }
            ?: FrameCatalog.themes(frameType).first()
        val orderedPaths = params.order.mapNotNull { i -> picked.getOrNull(i) }
        val targetWidth = if (frameLayoutId == FrameLayoutId.FOUR_VERTICAL) 1650 else Constants.RESULT_IMAGE_MAX_WIDTH
        val bitmaps = orderedPaths.mapNotNull { BitmapDecoding.decodeSampled(it, reqSize = targetWidth) }
        val dateString = if (params.showDate) todayString() else null
        val result = try {
            CollageRenderer.render(
                images = bitmaps,
                frameStyle = style,
                theme = theme,
                overrideBackground = frameColor.color,
                filterId = params.filterId,
                text = params.text.takeIf { it.isNotBlank() },
                dateString = dateString,
                outputWidth = targetWidth,
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
