package com.pocket4cut.data.storage

import android.graphics.Bitmap

/**
 * 이미지 저장 인터페이스
 */
interface ImageStorage {
    suspend fun saveCapture(
        imageBytes: ByteArray,
        sessionId: String,
        index: Int
    ): String

    suspend fun getCapturePaths(sessionId: String): List<String>

    suspend fun saveResult(
        bitmap: Bitmap,
        sessionId: String
    ): String

    suspend fun deleteSessionFiles(sessionId: String)
}

