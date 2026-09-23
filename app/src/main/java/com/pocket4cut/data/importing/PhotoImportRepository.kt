package com.pocket4cut.data.importing

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.AtomicFile
import androidx.core.net.toUri
import com.pocket4cut.data.local.SessionConflictException
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.data.local.SessionStorageException
import com.pocket4cut.domain.model.InputSource
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

enum class ImportJournalStatus { PREPARED, COPYING, FILE_PUBLISHED, SESSION_COMMITTED }

enum class PhotoImportFailureReason {
    TOO_MANY_SELECTIONS,
    TOO_LARGE,
    DIMENSIONS_TOO_LARGE,
    UNSUPPORTED_FORMAT,
    ANIMATED_IMAGE,
    UNREADABLE,
    INVALID_IMAGE,
    STORAGE,
    CONFLICT,
    RESELECT_REQUIRED,
}

data class PhotoImportFailure(
    val uri: String,
    val reason: PhotoImportFailureReason,
    val message: String,
)

data class PhotoImportBatchResult(
    val session: SessionDocument?,
    val importedPhotoIds: List<String> = emptyList(),
    val duplicates: List<String> = emptyList(),
    val failures: List<PhotoImportFailure> = emptyList(),
)

data class PhotoImportRecoveryResult(
    val session: SessionDocument?,
    val recoveredPhotoIds: List<String> = emptyList(),
    val needsReselection: Boolean = false,
    val failures: List<PhotoImportFailure> = emptyList(),
)

private data class ImportJournal(
    val sessionId: String,
    val photoId: String,
    val frameTypeId: String,
    val sourceUri: String,
    val status: ImportJournalStatus,
    val captureIndex: Int,
    val pendingPath: String,
    val finalPath: String? = null,
    val byteCount: Long? = null,
    val sha256: String? = null,
    val createdAt: Long,
    val persistableGrant: Boolean = false,
)

private data class RemovalJournal(
    val sessionId: String,
    val photoId: String,
    val path: String,
)

private data class ValidatedImport(
    val extension: String,
    val byteCount: Long,
    val sha256: String,
)

private enum class ImageKind(val extension: String) {
    JPEG("jpg"), PNG("png"), WEBP("webp"), HEIF("heic"),
}

private class ImportRejectedException(
    val reason: PhotoImportFailureReason,
    message: String,
) : IOException(message)

/**
 * Copies picker URIs into app-owned, non-destructive source files. Every transition is durable,
 * and replay is idempotent across process death. The external source is opened read-only.
 */
class PhotoImportRepository(
    private val context: Context,
    private val sessions: SessionDocumentRepository = SessionDocumentRepository(context),
) {
    private val resolver = context.contentResolver
    private val journalsRoot = File(context.filesDir, "import_journals")
    private val removalsRoot = File(context.filesDir, "import_removals")
    private val picturesRoot = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw SessionStorageException("App-owned picture storage is unavailable"),
        "Pocket4Cut",
    )

    suspend fun importUris(
        sessionId: String,
        frameTypeId: String,
        uris: List<Uri>,
    ): PhotoImportBatchResult = withContext(Dispatchers.IO) {
        processMutex.withLock {
            validateId(sessionId)
            val requiredCount = selectedCount(frameTypeId)
            val replay = recoverLocked(sessionId)
            var current = replay.session
            current?.let { document ->
                document.requireAlbumIdentity(frameTypeId)
                if (document.results.isNotEmpty()) throw SessionConflictException(sessionId)
                if (document.stage != SessionStage.IMPORT) {
                    current = sessions.update(sessionId, document.revision) {
                        it.copy(stage = SessionStage.IMPORT)
                    }
                }
            }

            val committedUris = readImportJournals(sessionId).map { it.sourceUri }.toMutableSet()
            val seenThisCall = mutableSetOf<String>()
            val duplicates = mutableListOf<String>()
            val newUris = uris.filter { uri ->
                val value = uri.toString()
                if (!seenThisCall.add(value) || value in committedUris) {
                    duplicates += value
                    false
                } else true
            }
            val remaining = requiredCount - (current?.photos?.size ?: 0)
            if (newUris.size > remaining) {
                val message = "선택 가능한 사진 수를 초과했습니다. ${remaining}장을 다시 선택해 주세요."
                return@withLock PhotoImportBatchResult(
                    session = current,
                    duplicates = duplicates,
                    failures = newUris.map {
                        PhotoImportFailure(it.toString(), PhotoImportFailureReason.TOO_MANY_SELECTIONS, message)
                    },
                )
            }

            val imported = mutableListOf<String>()
            val failures = replay.failures.toMutableList()
            newUris.forEach { uri ->
                val photoId = UUID.randomUUID().toString()
                val captureIndex = (current?.photos?.maxOfOrNull { it.captureIndex } ?: -1) + 1
                var journal = ImportJournal(
                    sessionId = sessionId,
                    photoId = photoId,
                    frameTypeId = frameTypeId,
                    sourceUri = uri.toString(),
                    status = ImportJournalStatus.PREPARED,
                    captureIndex = captureIndex,
                    pendingPath = "imports/$sessionId/.pending/$photoId.part",
                    createdAt = current?.createdAt ?: System.currentTimeMillis(),
                    persistableGrant = takePersistablePermission(uri),
                )
                writeJournal(journal)
                try {
                    journal = copyAndPublish(journal)
                    current = commitJournal(journal)
                    imported += photoId
                    committedUris += uri.toString()
                    releasePersistablePermission(journal)
                } catch (rejected: ImportRejectedException) {
                    discardRejected(journal)
                    failures += PhotoImportFailure(uri.toString(), rejected.reason,
                        rejected.message ?: "사진을 가져올 수 없습니다.")
                } catch (conflict: SessionConflictException) {
                    // Keep the journal and published file for an idempotent retry.
                    failures += PhotoImportFailure(uri.toString(), PhotoImportFailureReason.CONFLICT,
                        "작업이 동시에 변경되었습니다. 저장된 사진을 다시 확인해 주세요.")
                    current = sessions.getById(sessionId)
                } catch (error: Throwable) {
                    failures += PhotoImportFailure(uri.toString(), PhotoImportFailureReason.STORAGE,
                        error.message ?: "사진을 안전하게 저장하지 못했습니다.")
                    current = sessions.getById(sessionId)
                }
            }
            PhotoImportBatchResult(current, imported, duplicates, failures)
        }
    }

    suspend fun recover(sessionId: String): PhotoImportRecoveryResult = withContext(Dispatchers.IO) {
        processMutex.withLock {
            validateId(sessionId)
            recoverLocked(sessionId)
        }
    }

    /**
     * Replays every durable import/removal journal before the session gallery is scanned.
     * This includes the first-photo window where a file was published before the first session
     * document could be created.
     */
    suspend fun recoverAll(): List<PhotoImportRecoveryResult> = withContext(Dispatchers.IO) {
        processMutex.withLock {
            val sessionIds = sequenceOf(journalsRoot, removalsRoot)
                .flatMap { root -> root.listFiles().orEmpty().asSequence() }
                .filter(File::isDirectory)
                .map(File::getName)
                .distinct()
                .sorted()
                .toList()
            sessionIds.map { sessionId ->
                try {
                    validateId(sessionId)
                    val current = sessions.getByIdBlocking(sessionId)
                    val hasUnfinishedJournal = readImportJournals(sessionId)
                        .any { it.status != ImportJournalStatus.SESSION_COMMITTED }
                    if (current == null || hasUnfinishedJournal || removalFiles(sessionId).isNotEmpty()) {
                        recoverLocked(sessionId)
                    } else {
                        PhotoImportRecoveryResult(session = current)
                    }
                } catch (error: Throwable) {
                    PhotoImportRecoveryResult(
                        session = runCatching { sessions.getByIdBlocking(sessionId) }.getOrNull(),
                        failures = listOf(
                            PhotoImportFailure(
                                uri = "",
                                reason = PhotoImportFailureReason.STORAGE,
                                message = error.message ?: "앨범 가져오기 복구를 완료하지 못했습니다.",
                            ),
                        ),
                    )
                }
            }
        }
    }

    suspend fun removePhoto(
        sessionId: String,
        photoId: String,
        expectedRevision: Long,
    ): SessionDocument = withContext(Dispatchers.IO) {
        processMutex.withLock {
            validateId(sessionId)
            validateId(photoId)
            val current = sessions.getById(sessionId) ?: throw SessionStorageException("Session is missing")
            if (current.revision != expectedRevision) throw SessionConflictException(sessionId)
            current.requireAlbumIdentity(current.frameTypeId)
            if (current.results.isNotEmpty()) throw SessionConflictException(sessionId)
            val importDraft = if (current.stage == SessionStage.IMPORT) current else {
                sessions.update(sessionId, current.revision) { it.copy(stage = SessionStage.IMPORT) }
            }
            val photo = importDraft.photos.firstOrNull { it.photoId == photoId }
                ?: return@withLock importDraft
            val file = sessions.resolvePhotoPath(photo)
            requireImportFile(sessionId, file)
            val intent = RemovalJournal(sessionId, photoId, photo.path)
            writeRemoval(intent)
            replayRemoval(intent)
        }
    }

    private fun recoverLocked(sessionId: String): PhotoImportRecoveryResult {
        val failures = mutableListOf<PhotoImportFailure>()
        var needsReselection = false
        replayRemovals(sessionId, failures)
        var current = sessions.getByIdBlocking(sessionId)
        // A removal intent wins over an older committed import journal. If deletion could not be
        // completed, replaying imports in the same pass could resurrect a photo reference that the
        // user already removed. Keep both journals for a later safe retry and surface the failure.
        if (failures.isNotEmpty()) {
            return PhotoImportRecoveryResult(current, failures = failures)
        }
        val recovered = mutableListOf<String>()
        readImportJournals(sessionId).forEach { original ->
            var journal = original
            try {
                when (journal.status) {
                    ImportJournalStatus.PREPARED, ImportJournalStatus.COPYING -> {
                        recoverPublishedFile(journal)?.let { published ->
                            journal = published
                            val before = current?.photos?.any { it.photoId == journal.photoId } == true
                            current = commitJournal(journal)
                            if (!before) recovered += journal.photoId
                            releasePersistablePermission(journal)
                            return@forEach
                        }
                        val source = journal.sourceUri.toUri()
                        if (!canRead(source)) {
                            pendingFile(journal).delete()
                            deleteAtomic(journalFile(journal.sessionId, journal.photoId))
                            releasePersistablePermission(journal)
                            needsReselection = true
                            failures += PhotoImportFailure(journal.sourceUri,
                                PhotoImportFailureReason.RESELECT_REQUIRED,
                                "사진 접근 권한이 만료되었습니다. 사진을 다시 선택해 주세요.")
                            return@forEach
                        }
                        journal = copyAndPublish(journal)
                    }
                    ImportJournalStatus.FILE_PUBLISHED,
                    ImportJournalStatus.SESSION_COMMITTED,
                    -> validatePublished(journal)
                }
                val before = current?.photos?.any { it.photoId == journal.photoId } == true
                current = commitJournal(journal)
                if (!before) recovered += journal.photoId
                releasePersistablePermission(journal)
            } catch (rejected: ImportRejectedException) {
                discardRejected(journal)
                failures += PhotoImportFailure(journal.sourceUri, rejected.reason,
                    rejected.message ?: "가져오기 복구에 실패했습니다.")
            } catch (error: Throwable) {
                failures += PhotoImportFailure(journal.sourceUri, PhotoImportFailureReason.STORAGE,
                    error.message ?: "가져오기 복구를 완료하지 못했습니다.")
            }
        }
        current = sessions.getByIdBlocking(sessionId)
        return PhotoImportRecoveryResult(current, recovered, needsReselection, failures)
    }

    /** This method runs on Dispatchers.IO under the import process mutex. */
    private fun SessionDocumentRepository.getByIdBlocking(id: String): SessionDocument? =
        kotlinx.coroutines.runBlocking { getById(id) }

    private fun copyAndPublish(initial: ImportJournal): ImportJournal {
        recoverPublishedFile(initial)?.let { return it }
        var journal = initial.copy(status = ImportJournalStatus.COPYING)
        writeJournal(journal)
        val pending = pendingFile(journal)
        requireImportPendingFile(journal.sessionId, pending)
        pending.parentFile?.let { parent ->
            if (!parent.isDirectory && !parent.mkdirs()) throw SessionStorageException("Import directory is unavailable")
        }
        val source = try {
            resolver.openInputStream(journal.sourceUri.toUri())
                ?: throw ImportRejectedException(
                    PhotoImportFailureReason.UNREADABLE,
                    "선택한 사진을 읽을 수 없습니다.",
                )
        } catch (error: SecurityException) {
            throw ImportRejectedException(
                PhotoImportFailureReason.RESELECT_REQUIRED,
                "사진 접근 권한이 없어 다시 선택해야 합니다.",
            )
        } catch (error: IOException) {
            throw ImportRejectedException(
                PhotoImportFailureReason.UNREADABLE,
                error.message ?: "선택한 사진을 읽을 수 없습니다.",
            )
        }
        val validated = source.use { input ->
            FileOutputStream(pending, false).use { output ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = try {
                        input.read(buffer)
                    } catch (error: SecurityException) {
                        throw ImportRejectedException(
                            PhotoImportFailureReason.RESELECT_REQUIRED,
                            "사진 접근 권한이 없어 다시 선택해야 합니다.",
                        )
                    } catch (error: IOException) {
                        throw ImportRejectedException(
                            PhotoImportFailureReason.UNREADABLE,
                            error.message ?: "선택한 사진을 읽을 수 없습니다.",
                        )
                    }
                    if (count < 0) break
                    if (count == 0) continue
                    total += count
                    if (total > MAX_STREAM_BYTES) {
                        throw ImportRejectedException(
                            PhotoImportFailureReason.TOO_LARGE,
                            "사진 한 장은 64MiB 이하여야 합니다.",
                        )
                    }
                    digest.update(buffer, 0, count)
                    try {
                        output.write(buffer, 0, count)
                    } catch (error: IOException) {
                        throw SessionStorageException(
                            error.message ?: "가져온 사진을 앱 저장소에 쓰지 못했습니다.",
                        )
                    }
                }
                try {
                    output.fd.sync()
                } catch (error: IOException) {
                    throw SessionStorageException(
                        error.message ?: "가져온 사진을 앱 저장소에 동기화하지 못했습니다.",
                    )
                }
                inspectFile(pending, total, digest.digest().toHex())
            }
        }
        val finalPath = "imports/${journal.sessionId}/${journal.photoId}.${validated.extension}"
        val finalFile = File(picturesRoot, finalPath)
        requireImportFile(journal.sessionId, finalFile)
        if (finalFile.exists()) throw SessionConflictException(journal.sessionId)
        finalFile.parentFile?.let { parent ->
            if (!parent.isDirectory && !parent.mkdirs()) throw SessionStorageException("Import directory is unavailable")
        }
        if (!pending.renameTo(finalFile)) throw SessionStorageException("Could not publish imported photo")
        journal = journal.copy(
            status = ImportJournalStatus.FILE_PUBLISHED,
            finalPath = finalPath,
            byteCount = validated.byteCount,
            sha256 = validated.sha256,
        )
        writeJournal(journal)
        return journal
    }

    private fun commitJournal(initial: ImportJournal): SessionDocument {
        validatePublished(initial)
        var current = sessions.getByIdBlocking(initial.sessionId)
        if (current == null) {
            val target = selectedCount(initial.frameTypeId)
            current = kotlinx.coroutines.runBlocking {
                sessions.create(
                    SessionDocument(
                        sessionId = initial.sessionId,
                        createdAt = initial.createdAt,
                        captureCount = target,
                        selectedCount = target,
                        frameTypeId = initial.frameTypeId,
                        inputSource = InputSource.ALBUM,
                        stage = SessionStage.IMPORT,
                    ),
                )
            }
        }
        val path = initial.finalPath ?: throw SessionStorageException("Import path is missing")
        val photo = PhotoRef(initial.photoId, path, initial.captureIndex)
        val existing = current.photos.firstOrNull { it.photoId == initial.photoId }
        if (existing != null && existing != photo) throw SessionConflictException(initial.sessionId)
        if (existing == null) {
            current.requireAlbumContract(initial.frameTypeId)
            current = kotlinx.coroutines.runBlocking {
                sessions.appendImportedPhoto(initial.sessionId, current!!.revision, photo)
            }
        } else {
            current.requireAlbumIdentity(initial.frameTypeId)
        }
        if (initial.status != ImportJournalStatus.SESSION_COMMITTED) {
            writeJournal(initial.copy(status = ImportJournalStatus.SESSION_COMMITTED))
        }
        return current
    }

    private fun inspectFile(file: File, byteCount: Long, sha256: String): ValidatedImport {
        if (byteCount <= 0L || file.length() != byteCount) {
            throw ImportRejectedException(PhotoImportFailureReason.INVALID_IMAGE, "사진 파일이 비어 있거나 손상되었습니다.")
        }
        val header = ByteArray(64)
        val headerCount = FileInputStream(file).use { it.read(header) }.coerceAtLeast(0)
        val kind = detectKind(header.copyOf(headerCount))
        if (kind == ImageKind.JPEG && !hasCompleteJpegEnding(file)) {
            throw ImportRejectedException(
                PhotoImportFailureReason.INVALID_IMAGE,
                "JPEG 사진의 끝 표시가 없어 파일이 손상되었습니다.",
            )
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw ImportRejectedException(PhotoImportFailureReason.INVALID_IMAGE,
                "이 기기에서 사진을 해석할 수 없습니다.")
        }
        if (options.outWidth > MAX_LONG_EDGE || options.outHeight > MAX_LONG_EDGE) {
            throw ImportRejectedException(PhotoImportFailureReason.DIMENSIONS_TOO_LARGE,
                "사진의 긴 변은 32,768px 이하여야 합니다.")
        }
        if (options.outWidth.toLong() * options.outHeight.toLong() > MAX_PIXELS) {
            throw ImportRejectedException(PhotoImportFailureReason.DIMENSIONS_TOO_LARGE,
                "사진은 250MP 이하여야 합니다.")
        }
        var sampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / sampleSize > VALIDATION_LONG_EDGE) {
            sampleSize *= 2
        }
        val decoded = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            },
        ) ?: throw ImportRejectedException(
            PhotoImportFailureReason.INVALID_IMAGE,
            "사진의 실제 픽셀을 이 기기에서 해석할 수 없습니다.",
        )
        decoded.recycle()
        return ValidatedImport(kind.extension, byteCount, sha256)
    }

    /**
     * Android's JPEG decoder may return partial pixels for a stream truncated after SOS. Parse
     * marker lengths until a real entropy-stream EOI is found, while allowing Motion Photo/video
     * payloads and other bytes after that EOI.
     */
    private fun hasCompleteJpegEnding(file: File): Boolean {
        return BufferedInputStream(FileInputStream(file)).use { input ->
            fun readUnsignedShort(): Int? {
                val high = input.read()
                val low = input.read()
                return if (high < 0 || low < 0) null else (high shl 8) or low
            }

            fun skipExactly(byteCount: Int): Boolean {
                var remaining = byteCount.toLong()
                while (remaining > 0L) {
                    val skipped = input.skip(remaining)
                    if (skipped > 0L) {
                        remaining -= skipped
                    } else if (input.read() >= 0) {
                        remaining--
                    } else {
                        return false
                    }
                }
                return true
            }

            fun nextMarker(): Int? {
                while (true) {
                    var value: Int
                    do {
                        value = input.read()
                        if (value < 0) return null
                    } while (value != 0xff)
                    var marker: Int
                    do {
                        marker = input.read()
                        if (marker < 0) return null
                    } while (marker == 0xff)
                    if (marker != 0x00) return marker
                }
            }

            if (readUnsignedShort() != 0xffd8) return@use false
            var sawScan = false
            var pendingMarker: Int? = null
            while (true) {
                val marker = pendingMarker ?: nextMarker() ?: return@use false
                pendingMarker = null
                when {
                    marker == 0xd9 -> return@use sawScan
                    marker == 0xda -> {
                        val length = readUnsignedShort() ?: return@use false
                        if (length < 2 || !skipExactly(length - 2)) return@use false
                        sawScan = true
                        while (true) {
                            var value = input.read()
                            if (value < 0) return@use false
                            if (value != 0xff) continue
                            do {
                                value = input.read()
                                if (value < 0) return@use false
                            } while (value == 0xff)
                            when {
                                value == 0x00 || value in 0xd0..0xd7 -> Unit
                                value == 0xd9 -> return@use true
                                else -> {
                                    pendingMarker = value
                                    break
                                }
                            }
                        }
                    }
                    marker == 0xd8 || marker == 0x01 || marker in 0xd0..0xd7 -> Unit
                    else -> {
                        val length = readUnsignedShort() ?: return@use false
                        if (length < 2 || !skipExactly(length - 2)) return@use false
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            false
        }
    }

    private fun validatePublished(journal: ImportJournal) {
        val path = journal.finalPath ?: throw SessionStorageException("Published import path is missing")
        val file = File(picturesRoot, path)
        requireImportFile(journal.sessionId, file)
        if (!file.isFile) throw SessionStorageException("Published import file is missing")
        val expectedBytes = journal.byteCount ?: throw SessionStorageException("Import size is missing")
        val expectedHash = journal.sha256 ?: throw SessionStorageException("Import hash is missing")
        if (file.length() != expectedBytes || sha256(file) != expectedHash) {
            throw SessionStorageException("Published import file failed integrity validation")
        }
        inspectFile(file, expectedBytes, expectedHash)
    }

    /** Repairs the narrow rename-before-journal-write window after validating the orphan. */
    private fun recoverPublishedFile(journal: ImportJournal): ImportJournal? {
        if (journal.finalPath != null) return null
        val directory = File(picturesRoot, "imports/${journal.sessionId}")
        val matches = directory.listFiles().orEmpty().filter {
            it.isFile && it.nameWithoutExtension == journal.photoId &&
                it.extension.lowercase() in setOf("jpg", "png", "webp", "heic")
        }
        if (matches.isEmpty()) return null
        if (matches.size != 1) throw SessionStorageException("Ambiguous published import files")
        val file = matches.single()
        requireImportFile(journal.sessionId, file)
        val hash = sha256(file)
        val validated = inspectFile(file, file.length(), hash)
        if (file.extension.lowercase() != validated.extension) {
            throw SessionStorageException("Published import extension does not match its content")
        }
        return journal.copy(
            status = ImportJournalStatus.FILE_PUBLISHED,
            finalPath = "imports/${journal.sessionId}/${file.name}",
            byteCount = validated.byteCount,
            sha256 = validated.sha256,
        ).also(::writeJournal)
    }

    private fun detectKind(header: ByteArray): ImageKind {
        if (header.size >= 3 && header[0].u() == 0xff && header[1].u() == 0xd8 && header[2].u() == 0xff) {
            return ImageKind.JPEG
        }
        val png = intArrayOf(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        if (header.size >= png.size && png.indices.all { header[it].u() == png[it] }) return ImageKind.PNG
        if (header.size >= 21 && header.ascii(0, 4) == "RIFF" && header.ascii(8, 4) == "WEBP") {
            if (header.ascii(12, 4) == "VP8X" && (header[20].u() and 0x02) != 0) {
                throw ImportRejectedException(PhotoImportFailureReason.ANIMATED_IMAGE,
                    "움직이는 WebP는 지원하지 않습니다.")
            }
            return ImageKind.WEBP
        }
        if (header.size >= 12 && header.ascii(4, 4) == "ftyp") {
            val brands = header.ascii(8, header.size - 8)
            if (brands.contains("avif") || brands.contains("avis")) {
                throw ImportRejectedException(PhotoImportFailureReason.UNSUPPORTED_FORMAT,
                    "AVIF 사진은 지원하지 않습니다.")
            }
            if (listOf("heic", "heix", "hevc", "hevx", "heim", "heis", "mif1")
                    .any(brands::contains)
            ) return ImageKind.HEIF
        }
        if (header.size >= 6 && (header.ascii(0, 6) == "GIF87a" || header.ascii(0, 6) == "GIF89a")) {
            throw ImportRejectedException(PhotoImportFailureReason.ANIMATED_IMAGE, "GIF는 지원하지 않습니다.")
        }
        throw ImportRejectedException(PhotoImportFailureReason.UNSUPPORTED_FORMAT,
            "JPEG, PNG, 정적 WebP, HEIF/HEIC 사진만 지원합니다.")
    }

    private fun replayRemovals(sessionId: String, failures: MutableList<PhotoImportFailure>) {
        removalFiles(sessionId).forEach { file ->
            try {
                replayRemoval(readRemoval(sessionId, file))
            } catch (error: Throwable) {
                failures += PhotoImportFailure("", PhotoImportFailureReason.STORAGE,
                    error.message ?: "삭제 중이던 사진을 정리하지 못했습니다.")
            }
        }
    }

    private fun replayRemoval(intent: RemovalJournal): SessionDocument {
        validateId(intent.sessionId)
        validateId(intent.photoId)
        val file = File(picturesRoot, intent.path)
        // Validate ownership before changing the session document. A damaged intent must never
        // remove a reference from another session and only then discover an unsafe path.
        requireImportFile(intent.sessionId, file)
        val expectedPath = "imports/${intent.sessionId}/${file.name}"
        if (file.nameWithoutExtension != intent.photoId ||
            intent.path.replace('\\', '/') != expectedPath
        ) {
            throw SessionStorageException("Invalid import removal target")
        }
        var current = sessions.getByIdBlocking(intent.sessionId)
        val referencedPhoto = current?.photos?.firstOrNull { it.photoId == intent.photoId }
        if (referencedPhoto != null && referencedPhoto.path != intent.path) {
            throw SessionStorageException("Import removal path does not match the session")
        }
        if (referencedPhoto != null) {
            current = kotlinx.coroutines.runBlocking {
                sessions.removeImportedPhoto(intent.sessionId, current!!.revision, intent.photoId)
            }
        }
        if (file.exists() && !file.delete()) throw SessionStorageException("Could not delete imported photo")
        deleteAtomic(journalFile(intent.sessionId, intent.photoId))
        deleteAtomic(removalFile(intent.sessionId, intent.photoId))
        return current ?: throw SessionStorageException("Session was deleted while removing a photo")
    }

    private fun discardRejected(journal: ImportJournal) {
        pendingFile(journal).delete()
        journal.finalPath?.let { path ->
            val file = File(picturesRoot, path)
            requireImportFile(journal.sessionId, file)
            // A file already attached to the session is never discarded here.
            val attached = sessions.getByIdBlocking(journal.sessionId)?.photos?.any { it.photoId == journal.photoId } == true
            if (!attached) file.delete()
        }
        deleteAtomic(journalFile(journal.sessionId, journal.photoId))
        releasePersistablePermission(journal)
    }

    private fun takePersistablePermission(uri: Uri): Boolean = try {
        resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        true
    } catch (_: Exception) {
        false
    }

    private fun releasePersistablePermission(journal: ImportJournal) {
        if (!journal.persistableGrant) return
        try {
            resolver.releasePersistableUriPermission(
                journal.sourceUri.toUri(), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: Exception) {
            // The provider or OS may already have revoked the temporary grant.
        }
    }

    private fun canRead(uri: Uri): Boolean = try {
        resolver.openInputStream(uri)?.use { true } ?: false
    } catch (_: Exception) {
        false
    }

    private fun writeJournal(journal: ImportJournal) {
        val json = JSONObject().apply {
            put("sessionId", journal.sessionId)
            put("photoId", journal.photoId)
            put("frameTypeId", journal.frameTypeId)
            put("sourceUri", journal.sourceUri)
            put("status", journal.status.name)
            put("captureIndex", journal.captureIndex)
            put("pendingPath", journal.pendingPath)
            put("finalPath", journal.finalPath ?: JSONObject.NULL)
            put("byteCount", journal.byteCount ?: JSONObject.NULL)
            put("sha256", journal.sha256 ?: JSONObject.NULL)
            put("createdAt", journal.createdAt)
            put("persistableGrant", journal.persistableGrant)
        }
        writeAtomic(journalFile(journal.sessionId, journal.photoId), json.toString().toByteArray())
    }

    private fun readImportJournals(sessionId: String): List<ImportJournal> =
        journalFiles(sessionId).map { file ->
            val json = JSONObject(AtomicFile(file).openRead().bufferedReader().use { it.readText() })
            ImportJournal(
                sessionId = json.getString("sessionId"),
                photoId = json.getString("photoId"),
                frameTypeId = json.getString("frameTypeId"),
                sourceUri = json.getString("sourceUri"),
                status = ImportJournalStatus.valueOf(json.getString("status")),
                captureIndex = json.getInt("captureIndex"),
                pendingPath = json.getString("pendingPath"),
                finalPath = json.nullableString("finalPath"),
                byteCount = json.nullableLong("byteCount"),
                sha256 = json.nullableString("sha256"),
                createdAt = json.getLong("createdAt"),
                persistableGrant = json.optBoolean("persistableGrant", false),
            ).also {
                if (it.sessionId != sessionId || file.nameWithoutExtension != it.photoId) {
                    throw SessionStorageException("Invalid import journal identity")
                }
                validateId(it.photoId)
                selectedCount(it.frameTypeId)
            }
        }.sortedWith(compareBy<ImportJournal> { it.captureIndex }.thenBy { it.photoId })

    private fun writeRemoval(removal: RemovalJournal) {
        val json = JSONObject().apply {
            put("sessionId", removal.sessionId)
            put("photoId", removal.photoId)
            put("path", removal.path)
        }
        writeAtomic(removalFile(removal.sessionId, removal.photoId), json.toString().toByteArray())
    }

    private fun readRemoval(expectedSessionId: String, file: File): RemovalJournal {
        val json = JSONObject(AtomicFile(file).openRead().bufferedReader().use { it.readText() })
        return RemovalJournal(
            json.getString("sessionId"),
            json.getString("photoId"),
            json.getString("path"),
        ).also { removal ->
            validateId(removal.sessionId)
            validateId(removal.photoId)
            if (removal.sessionId != expectedSessionId || file.nameWithoutExtension != removal.photoId) {
                throw SessionStorageException("Invalid import removal journal identity")
            }
        }
    }

    private fun writeAtomic(file: File, bytes: ByteArray) {
        file.parentFile?.let { if (!it.isDirectory && !it.mkdirs()) throw SessionStorageException("Journal directory is unavailable") }
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try {
            output.write(bytes)
            atomic.finishWrite(output)
        } catch (error: Throwable) {
            atomic.failWrite(output)
            throw error
        }
    }

    private fun deleteAtomic(file: File) {
        listOf(File(file.path + ".new"), file, File(file.path + ".bak")).forEach { part ->
            if (part.exists() && !part.delete()) throw SessionStorageException("Could not clear import journal")
        }
        file.parentFile?.let { if (it.exists() && it.listFiles().isNullOrEmpty()) it.delete() }
    }

    private fun journalFiles(sessionId: String): List<File> = atomicFiles(File(journalsRoot, sessionId))
    private fun removalFiles(sessionId: String): List<File> = atomicFiles(File(removalsRoot, sessionId))
    private fun atomicFiles(directory: File): List<File> = directory.listFiles().orEmpty()
        .filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".json.bak")) }
        .map { if (it.name.endsWith(".bak")) File(it.path.removeSuffix(".bak")) else it }
        .distinctBy { it.path }

    private fun journalFile(sessionId: String, photoId: String): File =
        File(File(journalsRoot, sessionId), "$photoId.json")

    private fun removalFile(sessionId: String, photoId: String): File =
        File(File(removalsRoot, sessionId), "$photoId.json")

    private fun pendingFile(journal: ImportJournal): File = File(picturesRoot, journal.pendingPath)

    private fun requireImportPendingFile(sessionId: String, file: File) {
        val root = File(picturesRoot, "imports/$sessionId/.pending").canonicalFile
        if (file.canonicalFile.parentFile != root) throw SessionStorageException("Unsafe pending import path")
    }

    private fun requireImportFile(sessionId: String, file: File) {
        val root = File(picturesRoot, "imports/$sessionId").canonicalFile
        if (file.canonicalFile.parentFile != root) throw SessionStorageException("Unsafe import path")
    }

    private fun SessionDocument.requireAlbumContract(expectedFrameTypeId: String) {
        requireAlbumIdentity(expectedFrameTypeId)
        if (stage != SessionStage.IMPORT || results.isNotEmpty()) throw SessionConflictException(sessionId)
    }

    private fun SessionDocument.requireAlbumIdentity(expectedFrameTypeId: String) {
        if (inputSource != InputSource.ALBUM || frameTypeId != expectedFrameTypeId) {
            throw SessionConflictException(sessionId)
        }
    }

    private fun selectedCount(frameTypeId: String): Int = when (frameTypeId) {
        "2" -> 2
        "4" -> 4
        "6" -> 6
        else -> throw SessionStorageException("Unsupported frame type")
    }

    private fun validateId(value: String) {
        if (!value.matches(Regex("[A-Za-z0-9_-]{1,100}"))) {
            throw SessionStorageException("Invalid import identifier")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(FileInputStream(file)).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun Byte.u(): Int = toInt() and 0xff
    private fun ByteArray.ascii(start: Int, length: Int): String =
        if (start < 0 || length < 0 || start + length > size) ""
        else String(this, start, length, Charsets.ISO_8859_1)

    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)

    companion object {
        const val MAX_STREAM_BYTES = 64L * 1024L * 1024L
        const val MAX_LONG_EDGE = 32_768
        const val MAX_PIXELS = 250_000_000L
        private const val VALIDATION_LONG_EDGE = 1_024
        private val processMutex = Mutex()
    }
}
