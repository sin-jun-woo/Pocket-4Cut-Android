package com.pocket4cut.data.export

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.pocket4cut.core.util.FileUris
import com.pocket4cut.data.local.SessionConflictException
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.ExportOperation
import com.pocket4cut.domain.model.ExportStatus
import com.pocket4cut.domain.model.SessionDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

enum class ExportMode { AUTO, SAVE, SAVE_COPY }

sealed interface ExportOutcome {
    data class Saved(val uri: Uri) : ExportOutcome
    data class AlreadySaved(val uri: Uri) : ExportOutcome
    data class NeedsPermission(val permission: String) : ExportOutcome
    data class NeedsRecovery(val operationId: String) : ExportOutcome
    data class Failed(val message: String) : ExportOutcome
}

/**
 * A result ID has one normal gallery copy. Each step is recorded in the session document so a
 * retry after process death resumes the same MediaStore row instead of inserting another one.
 */
class GalleryExporter(
    private val context: Context,
    private val sessions: SessionDocumentRepository = SessionDocumentRepository(context),
) {
    suspend fun export(
        sessionId: String,
        resultId: String,
        mode: ExportMode = ExportMode.SAVE,
        requestId: String? = null,
    ): ExportOutcome = withContext(Dispatchers.IO) {
        exportMutex.withLock {
            if (Build.VERSION.SDK_INT <= 28 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
            ) return@withLock ExportOutcome.NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (mode == ExportMode.SAVE_COPY && requestId.isNullOrBlank()) {
                return@withLock ExportOutcome.Failed("A copy request ID is required")
            }
            val document = try { sessions.getById(sessionId) }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                return@withLock ExportOutcome.Failed(error.message ?: "Session unavailable")
            }
                ?: return@withLock ExportOutcome.Failed("Session unavailable")
            val result = document.results.firstOrNull { it.resultId == resultId }
                ?: return@withLock ExportOutcome.Failed("Result unavailable")
            if (result.legacy && mode == ExportMode.AUTO) {
                return@withLock ExportOutcome.Failed("Legacy results require an explicit save")
            }
            val source = try { sessions.resolveResultPath(result) }
            catch (error: Exception) { return@withLock ExportOutcome.Failed(error.message ?: "Result path invalid") }
            if (!source.isFile || source.length() <= 0) {
                return@withLock ExportOutcome.Failed("Result JPEG unavailable")
            }
            var preparedNow = false
            val operation = try {
                when (mode) {
                    ExportMode.SAVE_COPY -> {
                        document.exportOperations.firstOrNull { it.operationId == requestId }
                            ?.also {
                                if (it.resultId != resultId || !it.isCopy) {
                                    return@withLock ExportOutcome.Failed("Copy request conflict")
                                }
                            }
                            ?: prepare(sessionId, resultId, requestId!!, isCopy = true).also { preparedNow = true }
                    }
                    ExportMode.AUTO, ExportMode.SAVE -> {
                        document.exportOperations.asReversed().firstOrNull {
                            it.resultId == resultId && !it.isCopy && it.status != ExportStatus.FAILED
                        } ?: prepare(sessionId, resultId, UUID.randomUUID().toString(), isCopy = false)
                            .also { preparedNow = true }
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                return@withLock ExportOutcome.Failed(error.message ?: "Could not prepare gallery export")
            }
            try { complete(sessionId, source, operation, preparedNow) }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                ExportOutcome.Failed(error.message ?: "Gallery export failed")
            }
        }
    }

    /** Validates the app-owned result before granting read access through FileProvider. */
    suspend fun shareUri(sessionId: String, resultId: String): Uri = withContext(Dispatchers.IO) {
        val document = sessions.getById(sessionId) ?: error("Session unavailable")
        val result = document.results.firstOrNull { it.resultId == resultId } ?: error("Result unavailable")
        val source = sessions.resolveResultPath(result)
        require(source.isFile && source.length() > 0) { "Result JPEG unavailable" }
        val uri = FileUris.contentUriForFile(context, source)
        context.contentResolver.openInputStream(uri)?.use {
            require(it.read() >= 0) { "Result JPEG is empty" }
        } ?: error("Result share URI cannot be read")
        uri
    }

    private suspend fun prepare(
        sessionId: String, resultId: String, operationId: String, isCopy: Boolean,
    ): ExportOperation {
        require(operationId.matches(Regex("[A-Za-z0-9_-]{1,100}")))
        val op = ExportOperation(
            operationId = operationId,
            resultId = resultId,
            status = ExportStatus.PREPARED,
            displayName = "Pocket4Cut_${resultId}_${operationId}.jpg",
            createdAt = System.currentTimeMillis(),
            isCopy = isCopy,
        )
        mutate(sessionId) { document ->
            if (document.exportOperations.any { it.operationId == operationId }) {
                throw SessionConflictException(sessionId)
            }
            document.copy(exportOperations = document.exportOperations + op)
        }
        return op
    }

    private suspend fun complete(
        sessionId: String,
        source: File,
        initial: ExportOperation,
        preparedNow: Boolean,
    ): ExportOutcome {
        var op = initial
        if (op.status == ExportStatus.COMPLETED) {
            val uri = op.uri?.let(Uri::parse) ?: return ExportOutcome.NeedsRecovery(op.operationId)
            return if (isRecordedRow(op, uri) && sameBytes(source, uri) &&
                (Build.VERSION.SDK_INT < 29 || isPending(uri) == false)
            ) {
                ExportOutcome.AlreadySaved(uri)
            } else ExportOutcome.NeedsRecovery(op.operationId)
        }
        if (op.status == ExportStatus.FAILED) {
            return ExportOutcome.Failed("Previous gallery export failed")
        }
        if (op.status == ExportStatus.NEEDS_RECOVERY) {
            val matches = try { findMatchingRow(op.displayName) }
            catch (_: Exception) { return ExportOutcome.NeedsRecovery(op.operationId) }
            if (matches.isEmpty() && op.uri == null && canRetryMissingInsert(op)) {
                op = mark(sessionId, op.operationId, ExportStatus.PREPARED, null)
            } else if (matches.size != 1 || (op.uri != null && matches.single().toString() != op.uri)) {
                return ExportOutcome.NeedsRecovery(op.operationId)
            } else {
                val matched = matches.single()
                val verified = runCatching { sha256(source).contentEquals(sha256(matched)) }.getOrDefault(false)
                if (!verified) return ExportOutcome.NeedsRecovery(op.operationId)
                op = mark(sessionId, op.operationId, ExportStatus.COPIED, matched.toString())
            }
        }
        var uri = op.uri?.let(Uri::parse)
        if (uri != null && !isRecordedRow(op, uri)) return markRecovery(sessionId, op)
        if (uri == null) {
            val matched = try { findMatchingRow(op.displayName) }
            catch (_: Exception) { return markRecovery(sessionId, op) }
            if (matched.size > 1) return markRecovery(sessionId, op)
            uri = matched.singleOrNull()
            if (uri != null && Build.VERSION.SDK_INT <= 28 && op.uri == null && !preparedNow) {
                // Old MediaStore does not expose an owner package to verify an orphaned row.
                return markRecovery(sessionId, op)
            }
            if (uri == null) {
                if (!preparedNow && !canRetryMissingInsert(op)) return markRecovery(sessionId, op)
                uri = try { insert(op.displayName) }
                catch (error: Exception) {
                    mark(sessionId, op.operationId, ExportStatus.FAILED, null)
                    return ExportOutcome.Failed(error.message ?: "Could not create gallery entry")
                }
            }
            op = mark(sessionId, op.operationId, ExportStatus.INSERTED, uri.toString())
        }
        // An interrupted copy may already be complete. Hash verification avoids both a duplicate
        // insert and a second write into a publicly visible row.
        val bytesMatch = sameBytes(source, uri)
        if (!bytesMatch) {
            if (op.status == ExportStatus.PUBLISHED || op.status == ExportStatus.COMPLETED) {
                return markRecovery(sessionId, op)
            }
            if (Build.VERSION.SDK_INT >= 29 && isPending(uri) != true) {
                // Never overwrite an already public row whose bytes no longer match the result.
                return markRecovery(sessionId, op)
            }
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Could not open gallery output")
                if (!sha256(source).contentEquals(sha256(uri))) error("Gallery JPEG did not match the result")
            } catch (error: Exception) {
                return rollback(sessionId, op, uri, error)
            }
        }
        if (op.status == ExportStatus.PREPARED || op.status == ExportStatus.INSERTED) {
            op = mark(sessionId, op.operationId, ExportStatus.COPIED, uri.toString())
        }
        if (Build.VERSION.SDK_INT >= 29 && op.status == ExportStatus.COPIED) {
            val published = try {
                context.contentResolver.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null) == 1
            } catch (_: Exception) { false }
            if (!published) return markRecovery(sessionId, op)
        }
        if (op.status == ExportStatus.COPIED) {
            op = mark(sessionId, op.operationId, ExportStatus.PUBLISHED, uri.toString())
        }
        if (Build.VERSION.SDK_INT >= 29 && isPending(uri) != false) return markRecovery(sessionId, op)
        if (!readable(uri)) return markRecovery(sessionId, op)
        mark(sessionId, op.operationId, ExportStatus.COMPLETED, uri.toString())
        return ExportOutcome.Saved(uri)
    }

    private suspend fun rollback(
        sessionId: String, op: ExportOperation, uri: Uri, cause: Exception,
    ): ExportOutcome {
        val removed = try { context.contentResolver.delete(uri, null, null) == 1 }
        catch (_: Exception) { false }
        if (!removed) return markRecovery(sessionId, op)
        mark(sessionId, op.operationId, ExportStatus.FAILED, null)
        return ExportOutcome.Failed(cause.message ?: "Gallery copy failed")
    }

    private suspend fun markRecovery(sessionId: String, op: ExportOperation): ExportOutcome {
        mark(sessionId, op.operationId, ExportStatus.NEEDS_RECOVERY, op.uri)
        return ExportOutcome.NeedsRecovery(op.operationId)
    }

    private suspend fun mark(
        sessionId: String, operationId: String, status: ExportStatus, uri: String?,
    ): ExportOperation {
        val updated = mutate(sessionId) { document ->
            val operations = document.exportOperations.map {
                if (it.operationId == operationId) it.copy(status = status, uri = uri ?: it.uri) else it
            }
            document.copy(exportOperations = operations)
        }
        return updated.exportOperations.first { it.operationId == operationId }
    }

    private suspend fun mutate(
        sessionId: String, change: (SessionDocument) -> SessionDocument,
    ): SessionDocument {
        repeat(5) {
            val latest = sessions.getById(sessionId) ?: error("Session unavailable")
            try { return sessions.update(sessionId, latest.revision, change) }
            catch (_: SessionConflictException) { /* A concurrent edit won; retry from fresh revision. */ }
        }
        throw SessionConflictException(sessionId)
    }

    private fun insert(name: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pocket4Cut/")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Pocket4Cut")
                if (!directory.exists() && !directory.mkdirs()) error("Could not create public pictures directory")
                put(MediaStore.Images.Media.DATA, File(directory, name).absolutePath)
            }
        }
        return context.contentResolver.insert(collection(), values) ?: error("Gallery insert failed")
    }

    private fun findMatchingRow(name: String): List<Uri> {
        val projection = if (Build.VERSION.SDK_INT >= 29) {
            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.OWNER_PACKAGE_NAME,
                MediaStore.Images.Media.RELATIVE_PATH)
        } else arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA)
        val matches = mutableListOf<Uri>()
        val cursor = when {
            Build.VERSION.SDK_INT >= 30 -> context.contentResolver.query(collection(), projection,
                Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Images.Media.DISPLAY_NAME}=?")
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(name))
                    putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
                }, null)
            Build.VERSION.SDK_INT == 29 -> context.contentResolver.query(
                includePendingCollection(), projection,
                "${MediaStore.Images.Media.DISPLAY_NAME}=?", arrayOf(name), null)
            else -> context.contentResolver.query(collection(), projection,
                "${MediaStore.Images.Media.DISPLAY_NAME}=?", arrayOf(name), null)
        } ?: error("Could not inspect gallery entries")
        cursor.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                if (Build.VERSION.SDK_INT >= 29) {
                    val owner = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.OWNER_PACKAGE_NAME))
                    val directory = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH))
                    if (owner != context.packageName || directory != "Pictures/Pocket4Cut/") continue
                } else {
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                    val expected = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "Pocket4Cut/$name").absolutePath
                    if (path != expected) continue
                }
                matches += ContentUris.withAppendedId(collection(), cursor.getLong(idColumn))
            }
        }
        return matches
    }

    @Suppress("DEPRECATION") // API 29 has no QUERY_ARG_MATCH_PENDING.
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun includePendingCollection(): Uri = MediaStore.setIncludePending(collection())

    private fun canRetryMissingInsert(op: ExportOperation): Boolean {
        if (Build.VERSION.SDK_INT >= 29) return true // findMatchingRow includes pending rows.
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Pocket4Cut")
        return !File(directory, op.displayName).exists()
    }

    private fun isRecordedRow(op: ExportOperation, uri: Uri): Boolean =
        runCatching { findMatchingRow(op.displayName).singleOrNull() == uri }.getOrDefault(false)

    private fun isPending(uri: Uri): Boolean? = try {
        context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.IS_PENDING), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) null
                else cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_PENDING)) == 1
            }
    } catch (_: Exception) { null }

    private fun sameBytes(source: File, uri: Uri): Boolean =
        runCatching { sha256(source).contentEquals(sha256(uri)) }.getOrDefault(false)

    private fun collection(): Uri = if (Build.VERSION.SDK_INT >= 29) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    private fun readable(uri: Uri): Boolean = try {
        context.contentResolver.openInputStream(uri)?.use { it.read() >= 0 } ?: false
    } catch (_: Exception) { false }

    private fun sha256(file: File): ByteArray = file.inputStream().use { input -> digest(input) }
    private fun sha256(uri: Uri): ByteArray = context.contentResolver.openInputStream(uri)?.use { input ->
        digest(input)
    } ?: error("Gallery item cannot be read")
    private fun digest(input: java.io.InputStream): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val size = input.read(buffer)
            if (size < 0) return digest.digest()
            digest.update(buffer, 0, size)
        }
    }

    companion object { private val exportMutex = Mutex() }
}
