package com.pocket4cut.data.storage

import android.content.Context
import android.os.Environment
import android.graphics.Bitmap
import com.pocket4cut.core.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FileImageStorage(
    private val context: Context,
) : ImageStorage {
    override suspend fun saveCapture(
        imageBytes: ByteArray,
        sessionId: String,
        index: Int,
    ): String = withContext(Dispatchers.IO) {
        val dir = captureDir(sessionId).apply { mkdirs() }
        val file = File(dir, "cap_${index.toString().padStart(2, '0')}.jpg")
        FileOutputStream(file).use { it.write(imageBytes) }
        file.absolutePath
    }

    suspend fun createCaptureFile(sessionId: String, index: Int): File = withContext(Dispatchers.IO) {
        val dir = captureDir(sessionId).apply { mkdirs() }
        File(dir, "cap_${index.toString().padStart(2, '0')}.jpg")
    }

    override suspend fun getCapturePaths(sessionId: String): List<String> = withContext(Dispatchers.IO) {
        val dir = captureDir(sessionId)
        if (!dir.exists()) return@withContext emptyList()
        dir.listFiles()
            ?.sortedBy { it.name }
            ?.map { it.absolutePath }
            .orEmpty()
    }

    override suspend fun saveResult(bitmap: Bitmap, sessionId: String): String = withContext(Dispatchers.IO) {
        val dir = resultsDir().apply { mkdirs() }
        val file = File(dir, "${sessionId}_result.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.RESULT_IMAGE_QUALITY, out)
        }
        file.absolutePath
    }

    override suspend fun deleteSessionFiles(sessionId: String) = withContext(Dispatchers.IO) {
        captureDir(sessionId).deleteRecursively()
        File(resultsDir(), "${sessionId}_result.jpg").delete()
        Unit
    }

    private fun captureDir(sessionId: String): File = File(picturesBaseDir(), "captures/$sessionId")

    private fun resultsDir(): File = File(picturesBaseDir(), "results")

    private fun picturesBaseDir(): File =
        File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "Pocket4Cut",
        )
}

