package com.pocket4cut.data.storage

import android.content.Context
import android.os.Environment
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.pocket4cut.core.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.UUID

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

    suspend fun createPendingCaptureFile(sessionId: String): File = withContext(Dispatchers.IO) {
        val dir = File(captureDir(sessionId), ".pending").apply { mkdirs() }
        File(dir, "${UUID.randomUUID()}.jpg")
    }

    suspend fun publishCaptureFile(pending: File, sessionId: String, index: Int): File =
        withContext(Dispatchers.IO) {
            require(pending.parentFile?.canonicalFile == File(captureDir(sessionId), ".pending").canonicalFile)
            if (!pending.isFile || !isValidJpeg(pending)) {
                if (pending.exists() && !pending.delete()) error("Unable to discard invalid capture")
                error("Captured photo is not a valid JPEG")
            }
            val output = createCaptureFile(sessionId, index)
            check(!output.exists()) { "Capture slot already contains a photo" }
            check(pending.renameTo(output)) { "Unable to publish captured photo" }
            output
        }

    private fun isValidJpeg(file: File): Boolean {
        if (file.length() < 4L) return false
        val markersValid = try {
            RandomAccessFile(file, "r").use { input ->
                val start = input.readUnsignedShort()
                input.seek(input.length() - 2)
                start == 0xFFD8 && input.readUnsignedShort() == 0xFFD9
            }
        } catch (_: java.io.IOException) { false }
        if (!markersValid) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth > 0 && options.outHeight > 0
    }

    override suspend fun getCapturePaths(sessionId: String): List<String> = withContext(Dispatchers.IO) {
        val dir = captureDir(sessionId)
        if (!dir.exists()) return@withContext emptyList()
        dir.listFiles()
            ?.filter { it.isFile && Regex("cap_[0-9]+\\.jpg", RegexOption.IGNORE_CASE).matches(it.name) }
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

    suspend fun saveImmutableResult(bitmap: Bitmap, sessionId: String, resultId: String): String =
        withContext(Dispatchers.IO) {
            require(sessionId.matches(Regex("[A-Za-z0-9_-]+")))
            require(resultId.matches(Regex("[A-Za-z0-9_-]+")))
            val dir = resultsDir().apply { mkdirs() }
            val output = File(dir, "${sessionId}_${resultId}.jpg")
            check(!output.exists()) { "Result already exists" }
            val pending = File(dir, ".${sessionId}_${resultId}.tmp")
            try {
                FileOutputStream(pending).use { stream ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.RESULT_IMAGE_QUALITY, stream)) {
                        "JPEG encoding failed"
                    }
                    stream.fd.sync()
                }
                check(pending.length() > 0L)
                check(pending.renameTo(output)) { "Unable to publish result" }
            } finally {
                pending.delete()
            }
            output.absolutePath
        }

    suspend fun saveBitmapToPath(bitmap: Bitmap, absolutePath: String) = withContext(Dispatchers.IO) {
        val file = File(absolutePath)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.RESULT_IMAGE_QUALITY, out)
        }
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

