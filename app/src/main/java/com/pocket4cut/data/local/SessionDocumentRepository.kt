package com.pocket4cut.data.local

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.AtomicFile
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.CURRENT_SESSION_SCHEMA_VERSION
import com.pocket4cut.domain.model.InputSource
import com.pocket4cut.domain.model.ResultRecord
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.domain.model.inferFrameTypeId
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.occasion.OccasionCatalogContract
import com.pocket4cut.frame.occasion.OccasionCatalogException
import com.pocket4cut.frame.occasion.OccasionCatalogLoader
import com.pocket4cut.frame.occasion.OccasionSelectionContract
import com.pocket4cut.frame.occasion.OccasionSelectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed class SessionStoreException(message: String, cause: Throwable? = null) : IOException(message, cause)
class SessionMissingException(id: String) : SessionStoreException("Session $id does not exist")
class SessionConflictException(id: String) : SessionStoreException("Session $id was updated by another operation")
class SessionCorruptException(id: String, cause: Throwable? = null) :
    SessionStoreException("Session $id needs recovery", cause)
class UnsupportedSessionVersionException(val version: Int) :
    SessionStoreException("Unsupported session format version $version")
class SessionStorageException(message: String, cause: Throwable? = null) : SessionStoreException(message, cause)

enum class ResultPublicationIssueKind {
    MISSING_JPEG, INVALID_JPEG, INVALID_JOURNAL, CONFLICT, QUARANTINE_INCOMPLETE,
}

data class ResultPublicationIssue(val resultId: String, val kind: ResultPublicationIssueKind)

/** Gallery can show damaged documents individually while the strict list() API still fails. */
data class SessionScan(
    val documents: List<SessionDocument>,
    val unreadableSessionIds: List<String>,
    /** Legacy data remains untouched when migration fails; healthy new documents stay visible. */
    val legacyMigrationError: SessionStoreException? = null,
)

/**
 * One document per session. All instances share the same process lock; AtomicFile protects an
 * interrupted write, while a validated last-good copy protects against syntactically bad data.
 */
class SessionDocumentRepository(
    private val context: Context,
    private val occasionThemeIdsProvider: () -> Set<String> = {
        OccasionCatalogLoader.load(context.applicationContext).themesById.keys
    },
) {
    private val documentsDir = File(context.filesDir, "session_documents")
    private val tombstonesDir = File(documentsDir, "tombstones")
    private val hiddenDir = File(documentsDir, "hidden")
    private val resultPublicationsDir = File(context.filesDir, "result_publications")
    private val quarantineIntentsDir = File(context.filesDir, "result_quarantine_intents")
    private val quarantinedPublicationsDir = File(context.filesDir, "result_publication_quarantine")
    private val picturesRoot = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw SessionStorageException("App-owned picture storage is unavailable"),
        "Pocket4Cut",
    )
    private val knownOccasionThemeIds by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        occasionThemeIdsProvider()
    }

    suspend fun create(document: SessionDocument): SessionDocument = ioLocked {
        validate(document)
        if (document.revision != 0L) throw SessionStorageException("New session revision must be zero")
        if (atomicExists(documentFile(document.sessionId)) || tombstoneFile(document.sessionId).exists() ||
            atomicExists(hiddenFile(document.sessionId))) {
            throw SessionConflictException(document.sessionId)
        }
        val dated = if (document.draft.dateText.isBlank()) {
            document.copy(draft = document.draft.copy(dateText = sessionDate(document.createdAt)))
        } else document
        writeDocumentLocked(dated)
        emit(dated)
        dated
    }

    suspend fun getById(id: String): SessionDocument? = ioLocked {
        validateId(id)
        if (atomicExists(hiddenFile(id))) {
            emit(null, id)
            return@ioLocked null
        }
        if (!atomicExists(documentFile(id)) && !tombstoneFile(id).exists()) migrateLegacyLocked()
        val publicationNeedsRecovery = !tombstoneFile(id).exists() && replayResultPublicationsLocked(id)
        readDocumentLocked(id)?.takeIf {
            it.stage == SessionStage.DELETED || tombstoneFile(id).exists()
        }?.let { finishDeleteLocked(it) }
        val document = if (tombstoneFile(id).exists()) null else readDocumentLocked(id)?.let {
            if (publicationNeedsRecovery) it.copy(stage = SessionStage.NEEDS_RECOVERY) else it
        }
        emit(document, id)
        document
    }

    suspend fun list(): List<SessionDocument> = ioLocked {
        migrateLegacyLocked()
        val documents = readAllLocked()
        documents.filter {
            it.stage == SessionStage.DELETED || tombstoneFile(it.sessionId).exists()
        }.forEach { finishDeleteLocked(it) }
        val publicationRecoveryIds = documents
            .filter { it.stage != SessionStage.DELETED && !tombstoneFile(it.sessionId).exists() }
            .mapNotNull { if (replayResultPublicationsLocked(it.sessionId)) it.sessionId else null }
            .toSet()
        readAllLocked().filter { it.stage != SessionStage.DELETED }
            .map { if (it.sessionId in publicationRecoveryIds) it.copy(stage = SessionStage.NEEDS_RECOVERY) else it }
            .sortedByDescending { it.updatedAt }
    }

    suspend fun scanForGallery(): SessionScan = ioLocked {
        val legacyMigrationError = try {
            migrateLegacyLocked()
            null
        } catch (error: SessionStoreException) {
            error
        }
        val ids = documentsDir.listFiles().orEmpty()
            .filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".json.bak")) }
            .map { it.name.removeSuffix(".bak").removeSuffix(".json") }
            .distinct()
        val documents = mutableListOf<SessionDocument>()
        val unreadable = mutableListOf<String>()
        ids.forEach { id ->
            if (atomicExists(hiddenFile(id))) return@forEach
            val first = try { readDocumentLocked(id) }
            catch (_: SessionStoreException) {
                unreadable += id
                null
            } ?: return@forEach
            if (first.stage == SessionStage.DELETED || tombstoneFile(id).exists()) {
                // Keep the deletion intent until the legacy index can be checked safely.
                if (legacyMigrationError == null) finishDeleteLocked(first)
                return@forEach
            }
            val visible = try {
                val publicationNeedsRecovery = replayResultPublicationsLocked(id)
                readDocumentLocked(id)?.let { current ->
                    if (publicationNeedsRecovery) current.copy(stage = SessionStage.NEEDS_RECOVERY)
                    else current
                }
            } catch (_: SessionStoreException) {
                unreadable += id
                null
            }
            if (visible != null) documents += visible
        }
        SessionScan(documents.sortedByDescending { it.updatedAt }, unreadable, legacyMigrationError)
    }

    /** Hide an unreadable record while retaining its JSON, photos, results and export history. */
    suspend fun hideUnreadableSession(id: String) = ioLocked {
        validateId(id)
        if (atomicExists(hiddenFile(id))) return@ioLocked
        if (tombstoneFile(id).exists()) throw SessionStorageException("Session deletion is in progress")
        if (!atomicExists(documentFile(id))) throw SessionMissingException(id)
        val readable = try { readDocumentLocked(id); true }
        catch (_: SessionStoreException) { false }
        if (readable) throw SessionStorageException("Readable sessions cannot be hidden as damaged")
        writeAtomic(hiddenFile(id), id.toByteArray(StandardCharsets.UTF_8))
        emit(null, id)
    }

    suspend fun listHiddenSessionIds(): List<String> = ioLocked { hiddenSessionIdsLocked() }

    /** Restores visibility only; no session JSON or photo is rewritten. */
    suspend fun unhideSession(id: String) = ioLocked {
        validateId(id)
        val marker = hiddenFile(id)
        if (!atomicExists(marker)) throw SessionMissingException(id)
        if (!atomicExists(documentFile(id))) throw SessionStorageException("Preserved session document is missing")
        listOf(File(marker.path + ".new"), marker, File(marker.path + ".bak")).forEach { part ->
            if (part.exists() && !part.delete()) throw SessionStorageException("Could not restore hidden session")
        }
        emit(null, id)
    }

    private fun hiddenSessionIdsLocked(): List<String> = hiddenDir.listFiles().orEmpty()
        .filter { it.isFile && (it.name.endsWith(".hidden") || it.name.endsWith(".hidden.bak")) }
        .map { it.name.removeSuffix(".bak").removeSuffix(".hidden") }
        .filter { it.matches(Regex("[A-Za-z0-9_-]{1,100}")) }
        .distinct()
        .sorted()

    fun observe(id: String): Flow<SessionDocument?> {
        validateId(id)
        return flow {
            getById(id) // Loads the persisted value and publishes it under the process lock.
            emitAll(flows.getOrPut(key(id)) { MutableStateFlow(null) }.asStateFlow())
        }
    }

    suspend fun update(
        id: String,
        expectedRevision: Long,
        transform: (SessionDocument) -> SessionDocument,
    ): SessionDocument = ioLocked {
        validateId(id)
        if (replayResultPublicationsLocked(id)) throw SessionCorruptException(id)
        val current = readDocumentLocked(id) ?: throw SessionMissingException(id)
        if (current.stage == SessionStage.DELETED || tombstoneFile(id).exists()) {
            throw SessionMissingException(id)
        }
        if (current.stage == SessionStage.NEEDS_RECOVERY) throw SessionCorruptException(id)
        if (current.revision != expectedRevision) throw SessionConflictException(id)
        val changed = transform(current)
        if (changed.sessionId != id || changed.schemaVersion != CURRENT_SESSION_SCHEMA_VERSION ||
            changed.createdAt != current.createdAt || changed.revision != expectedRevision ||
            changed.frameTypeId != current.frameTypeId || changed.inputSource != current.inputSource ||
            changed.captureCount != current.captureCount || changed.selectedCount != current.selectedCount
        ) throw SessionStorageException("Immutable session identity or revision was changed")
        if (changed.stage == SessionStage.DELETED ||
            !changed.photos.containsAll(current.photos) || !changed.results.containsAll(current.results)
        ) throw SessionStorageException("Existing photos and results cannot be replaced by an edit")
        val next = changed.copy(revision = expectedRevision + 1, updatedAt = System.currentTimeMillis())
        validate(next)
        writeDocumentLocked(next)
        emit(next)
        next
    }

    suspend fun publishResult(
        id: String,
        expectedRevision: Long,
        result: ResultRecord,
    ): SessionDocument = update(id, expectedRevision) { current ->
        if (current.results.any { it.resultId == result.resultId }) throw SessionConflictException(id)
        if (current.photos.any { it.path == result.path } || current.results.any { it.path == result.path }) {
            throw SessionStorageException("Result file path must be unique")
        }
        val file = resolveResultPath(result)
        if (!file.isFile || !isJpeg(file)) throw SessionStorageException("Result JPEG is missing or invalid")
        if (!result.legacy && file.parentFile?.canonicalFile != File(picturesRoot, "results").canonicalFile) {
            throw SessionStorageException("Result is outside the results directory")
        }
        current.copy(stage = SessionStage.RESULT, results = current.results + result)
    }

    /** Durable intent is written before the JPEG is created; a later read can finish publication. */
    suspend fun prepareResultPublication(
        id: String,
        expectedRevision: Long,
        result: ResultRecord,
    ): ResultRecord = ioLocked {
        validatePublication(id, result)
        if (replayResultPublicationsLocked(id)) throw SessionCorruptException(id)
        val current = readDocumentLocked(id) ?: throw SessionMissingException(id)
        if (current.stage == SessionStage.DELETED || tombstoneFile(id).exists()) throw SessionMissingException(id)
        if (current.stage == SessionStage.NEEDS_RECOVERY) throw SessionCorruptException(id)
        if (current.revision != expectedRevision || result.sourceRevision != expectedRevision) {
            throw SessionConflictException(id)
        }
        if (current.results.any { it.resultId == result.resultId || it.path == result.path }) {
            throw SessionConflictException(id)
        }
        val journal = publicationFile(id, result.resultId)
        if (atomicExists(journal)) {
            if (readPublicationLocked(id, result.resultId) != result) throw SessionConflictException(id)
        } else {
            if (resolveResultPath(result).exists()) throw SessionConflictException(id)
            writeAtomic(journal, encodePublication(result).toByteArray(StandardCharsets.UTF_8))
        }
        result
    }

    /** Replaying this operation after process death or a completed publication is idempotent. */
    suspend fun completeResultPublication(id: String, resultId: String): SessionDocument = ioLocked {
        completeResultPublicationLocked(id, resultId, requireFile = true)
            ?: throw SessionStorageException("Result JPEG has not been published")
    }

    /** Lists unresolved result publications without treating a damaged session document as repairable. */
    suspend fun listResultPublicationIssues(id: String): List<ResultPublicationIssue> = ioLocked {
        validateId(id)
        val current = readDocumentLocked(id) ?: throw SessionMissingException(id)
        if (current.stage == SessionStage.NEEDS_RECOVERY || tombstoneFile(id).exists()) {
            throw SessionCorruptException(id)
        }
        replayResultPublicationsLocked(id)
        val unfinished = quarantineIntentIdsLocked(id).toSet()
        (publicationIdsLocked(id) + unfinished).distinct().map { resultId ->
            ResultPublicationIssue(resultId, if (resultId in unfinished) {
                ResultPublicationIssueKind.QUARANTINE_INCOMPLETE
            } else publicationIssueKindLocked(id, resultId))
        }
    }

    /**
     * Explicitly abandons only an unpublished result candidate. Its journal and JPEG are moved to
     * app-owned quarantine, not erased. A durable intent lets the next read finish an interrupted
     * move. Existing results and captured originals are never moved.
     */
    suspend fun quarantineResultPublication(id: String, resultId: String): SessionDocument = ioLocked {
        validateId(id)
        validateId(resultId)
        val current = readDocumentLocked(id) ?: throw SessionMissingException(id)
        if (current.stage == SessionStage.NEEDS_RECOVERY || tombstoneFile(id).exists()) {
            throw SessionCorruptException(id)
        }
        if (resultId !in publicationIdsLocked(id) && !atomicExists(quarantineIntentFile(id, resultId))) {
            throw SessionStorageException("Result publication issue is missing")
        }
        if (!atomicExists(quarantineIntentFile(id, resultId))) {
            val issue = publicationIssueKindLocked(id, resultId)
            if (current.results.any { it.resultId == resultId } && issue != ResultPublicationIssueKind.CONFLICT) {
                throw SessionStorageException("A completed result cannot be quarantined")
            }
            writeAtomic(quarantineIntentFile(id, resultId), resultId.toByteArray(StandardCharsets.UTF_8))
        }
        finishQuarantineLocked(id, resultId)
        val remaining = replayResultPublicationsLocked(id)
        val restored = readDocumentLocked(id) ?: throw SessionMissingException(id)
        val visible = if (remaining) restored.copy(stage = SessionStage.NEEDS_RECOVERY) else restored
        emit(visible)
        visible
    }

    private fun replayResultPublicationsLocked(id: String): Boolean {
        val current = readDocumentLocked(id) ?: return false
        if (current.stage == SessionStage.DELETED || current.stage == SessionStage.NEEDS_RECOVERY ||
            tombstoneFile(id).exists()) return false
        var needsRecovery = false
        quarantineIntentIdsLocked(id).forEach { resultId ->
            try { finishQuarantineLocked(id, resultId) }
            catch (_: SessionStoreException) { needsRecovery = true }
        }
        val unfinishedQuarantines = quarantineIntentIdsLocked(id).toSet()
        publicationIdsLocked(id).forEach { resultId ->
            if (resultId in unfinishedQuarantines) {
                needsRecovery = true
                return@forEach
            }
            try {
                if (completeResultPublicationLocked(id, resultId, requireFile = false) == null) {
                    needsRecovery = true
                }
            }
            catch (_: SessionStoreException) { needsRecovery = true }
        }
        return needsRecovery
    }

    private fun publicationIssueKindLocked(id: String, resultId: String): ResultPublicationIssueKind {
        val record = try { readPublicationLocked(id, resultId) }
        catch (_: SessionStoreException) { return ResultPublicationIssueKind.INVALID_JOURNAL }
            ?: return ResultPublicationIssueKind.INVALID_JOURNAL
        val current = readDocumentLocked(id) ?: throw SessionMissingException(id)
        if (current.results.any { it.resultId == resultId && it != record } ||
            current.photos.any { resolvePhotoPath(it).canonicalFile == resolveResultPath(record).canonicalFile } ||
            current.results.any { it.resultId != resultId &&
                resolveResultPath(it).canonicalFile == resolveResultPath(record).canonicalFile }
        ) return ResultPublicationIssueKind.CONFLICT
        val file = resolveResultPath(record)
        return when {
            !file.isFile -> ResultPublicationIssueKind.MISSING_JPEG
            !resultJpegMatchesRecord(file, record) -> ResultPublicationIssueKind.INVALID_JPEG
            else -> ResultPublicationIssueKind.CONFLICT // A valid file should have replayed already.
        }
    }

    private fun quarantineIntentFile(id: String, resultId: String): File =
        File(File(quarantineIntentsDir, id), "$resultId.json")

    private fun quarantineIntentIdsLocked(id: String): List<String> = File(quarantineIntentsDir, id)
        .listFiles().orEmpty()
        .filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".json.bak")) }
        .map { it.name.removeSuffix(".bak").removeSuffix(".json") }
        .distinct()

    private fun finishQuarantineLocked(id: String, resultId: String) {
        validateId(id)
        validateId(resultId)
        val intent = quarantineIntentFile(id, resultId)
        if (!atomicExists(intent)) return
        val current = readDocumentLocked(id) ?: throw SessionMissingException(id)
        val candidate = File(picturesRoot, "results/${id}_${resultId}.jpg")
        val isCompleted = current.results.any { result ->
            result.resultId == resultId || resolveResultPath(result).canonicalFile == candidate.canonicalFile
        }
        if (!isCompleted) {
            moveToQuarantineLocked(candidate,
                File(picturesRoot, "recovery_quarantine/$id/$resultId.jpg"))
        }
        val resultRoot = File(picturesRoot, "results").canonicalFile
        val pendingJpeg = File(resultRoot, ".${id}_${resultId}.tmp")
        if (pendingJpeg.exists()) {
            if (pendingJpeg.canonicalFile.parentFile != resultRoot) {
                throw SessionStorageException("Unsafe pending result path")
            }
            moveToQuarantineLocked(pendingJpeg,
                File(picturesRoot, "recovery_quarantine/$id/$resultId.tmp"))
        }
        val sourceJournal = publicationFile(id, resultId)
        val targetJournal = File(File(quarantinedPublicationsDir, id), resultId)
        listOf("", ".bak", ".new").forEach { suffix ->
            moveToQuarantineLocked(File(sourceJournal.path + suffix),
                File(targetJournal, "publication.json$suffix"))
        }
        val journalDirectory = publicationDirectory(id)
        if (journalDirectory.listFiles().isNullOrEmpty()) journalDirectory.delete()
        listOf(".new", ".bak", "").forEach { suffix ->
            val part = File(intent.path + suffix)
            if (part.exists() && !part.delete()) {
                throw SessionStorageException("Could not finish result quarantine")
            }
        }
        intent.parentFile?.let { if (it.listFiles().isNullOrEmpty()) it.delete() }
    }

    private fun moveToQuarantineLocked(source: File, destination: File) {
        if (!source.exists()) return
        if (destination.exists()) throw SessionStorageException("Result quarantine target already exists")
        if (destination.parentFile?.mkdirs() == false && destination.parentFile?.isDirectory != true) {
            throw SessionStorageException("Could not create result quarantine directory")
        }
        if (!source.renameTo(destination)) throw SessionStorageException("Could not quarantine result artifact")
    }

    private fun completeResultPublicationLocked(
        id: String, resultId: String, requireFile: Boolean,
    ): SessionDocument? {
        validateId(id)
        validateId(resultId)
        val current = readDocumentLocked(id) ?: throw SessionMissingException(id)
        if (current.stage == SessionStage.DELETED || tombstoneFile(id).exists()) throw SessionMissingException(id)
        if (current.stage == SessionStage.NEEDS_RECOVERY) throw SessionCorruptException(id)
        val record = readPublicationLocked(id, resultId)
        if (record == null) {
            return current.takeIf { doc -> doc.results.any { it.resultId == resultId } }
                ?: throw SessionStorageException("Result publication record is missing")
        }
        current.results.firstOrNull { it.resultId == resultId }?.let { existing ->
            if (existing != record) throw SessionConflictException(id)
            deletePublicationLocked(id, resultId)
            return current
        }
        val file = resolveResultPath(record)
        if (!file.isFile) {
            if (requireFile) throw SessionStorageException("Result JPEG has not been published")
            return null
        }
        if (!resultJpegMatchesRecord(file, record)) throw SessionCorruptException(id)
        if (current.photos.any { resolvePhotoPath(it).canonicalFile == file.canonicalFile } ||
            current.results.any { resolveResultPath(it).canonicalFile == file.canonicalFile }
        ) throw SessionConflictException(id)
        val next = current.copy(
            revision = current.revision + 1,
            updatedAt = System.currentTimeMillis(),
            stage = if (current.revision == record.sourceRevision) SessionStage.RESULT else current.stage,
            results = current.results + record,
        )
        validate(next)
        writeDocumentLocked(next)
        emit(next)
        deletePublicationLocked(id, resultId)
        return next
    }

    /** A completed capture file is checked before the photo becomes visible in the document. */
    suspend fun appendCapture(
        id: String,
        expectedRevision: Long,
        photo: PhotoRef,
    ): SessionDocument = update(id, expectedRevision) { current ->
        if (current.inputSource != InputSource.CAMERA) {
            throw SessionStorageException("Captured photos require a camera session")
        }
        if (current.photos.any { it.captureIndex == photo.captureIndex || it.photoId == photo.photoId }) {
            throw SessionConflictException(id)
        }
        val file = resolvePhotoPath(photo)
        if (!file.isFile || !isJpeg(file)) throw SessionStorageException("Capture JPEG is missing or invalid")
        if (!photo.legacy && file.parentFile?.canonicalFile != File(picturesRoot, "captures/$id").canonicalFile) {
            throw SessionStorageException("Capture is outside the session directory")
        }
        current.copy(photos = current.photos + photo)
    }

    /** Publishes one already-validated app-owned import into an album draft exactly once. */
    suspend fun appendImportedPhoto(
        id: String,
        expectedRevision: Long,
        photo: PhotoRef,
    ): SessionDocument = update(id, expectedRevision) { current ->
        if (current.inputSource != InputSource.ALBUM || current.stage != SessionStage.IMPORT) {
            throw SessionStorageException("Imported photos require an album import session")
        }
        current.photos.firstOrNull { it.photoId == photo.photoId }?.let { existing ->
            if (existing == photo) return@update current
            throw SessionConflictException(id)
        }
        if (current.photos.any { it.captureIndex == photo.captureIndex }) throw SessionConflictException(id)
        if (current.photos.size >= current.selectedCount) {
            throw SessionStorageException("The album draft already contains the required photos")
        }
        val file = resolvePhotoPath(photo)
        val importDirectory = File(picturesRoot, "imports/$id").canonicalFile
        if (photo.legacy || !file.isFile || file.parentFile?.canonicalFile != importDirectory) {
            throw SessionStorageException("Imported photo is outside the session directory")
        }
        val nextOrder = current.draft.selectedPhotoIdsInOrder + photo.photoId
        current.copy(
            photos = current.photos + photo,
            draft = current.draft.copy(selectedPhotoIdsInOrder = nextOrder),
        )
    }

    /** Removes a photo reference before the import service erases its app-owned copy. */
    suspend fun removeImportedPhoto(
        id: String,
        expectedRevision: Long,
        photoId: String,
    ): SessionDocument = ioLocked {
        validateId(id)
        validateId(photoId)
        val current = readDocumentLocked(id) ?: throw SessionMissingException(id)
        if (current.revision != expectedRevision) throw SessionConflictException(id)
        if (current.inputSource != InputSource.ALBUM || current.stage != SessionStage.IMPORT ||
            current.results.isNotEmpty()
        ) throw SessionStorageException("Only an unfinished album import can remove a photo")
        val removed = current.photos.firstOrNull { it.photoId == photoId }
            ?: return@ioLocked current
        val file = resolvePhotoPath(removed)
        if (removed.legacy || file.parentFile?.canonicalFile !=
            File(picturesRoot, "imports/$id").canonicalFile
        ) throw SessionStorageException("Imported photo is outside the session directory")
        val next = current.copy(
            revision = current.revision + 1,
            updatedAt = System.currentTimeMillis(),
            photos = current.photos.filterNot { it.photoId == photoId },
            draft = current.draft.copy(
                selectedPhotoIdsInOrder = current.draft.selectedPhotoIdsInOrder.filterNot { it == photoId },
                adjustmentsByPhotoId = current.draft.adjustmentsByPhotoId - photoId,
            ),
        )
        validate(next)
        writeDocumentLocked(next)
        emit(next)
        next
    }

    /** Keeps prior results and source photos when an edit draft is abandoned. */
    suspend fun discardDraft(id: String, expectedRevision: Long): SessionDocument? {
        val current = getById(id) ?: return null
        if (current.results.isEmpty()) {
            if (current.revision != expectedRevision) throw SessionConflictException(id)
            requestDelete(id, expectedRevision)
            return null
        }
        return update(id, expectedRevision) {
            it.copy(stage = SessionStage.RESULT, draft = SessionDraft(dateText = it.draft.dateText))
        }
    }

    /** Reads the last-good copy only when the main document is damaged. It never hides data loss. */
    suspend fun recover(id: String): SessionDocument? = ioLocked {
        validateId(id)
        if (tombstoneFile(id).exists()) return@ioLocked null
        val main = documentFile(id)
        if (!atomicExists(main)) return@ioLocked null
        val latest = runCatching { decodeValidated(id, AtomicFile(main).openRead().use { it.readBytes() }) }
        if (latest.isSuccess) return@ioLocked latest.getOrThrow()
        if (latest.exceptionOrNull() is UnsupportedSessionVersionException) throw latest.exceptionOrNull()!!
        if (latest.exceptionOrNull() is OccasionCatalogException) throw latest.exceptionOrNull()!!
        val lastGood = lastGoodFile(id)
        if (!atomicExists(lastGood)) throw SessionCorruptException(id, latest.exceptionOrNull())
        val recovered = try {
            decodeValidated(id, AtomicFile(lastGood).openRead().use { it.readBytes() })
        } catch (cause: UnsupportedSessionVersionException) {
            throw cause
        } catch (cause: OccasionCatalogException) {
            // Leave both files untouched so a later catalog retry can recover this session.
            throw cause
        } catch (cause: Throwable) {
            throw SessionCorruptException(id, cause)
        }
        val damaged = File(documentsDir, "$id.corrupt.${System.currentTimeMillis()}")
        if (!main.renameTo(damaged)) throw SessionStorageException("Could not preserve damaged session $id")
        try {
            writeAtomic(main, SessionDocumentCodec.encode(recovered).toByteArray(StandardCharsets.UTF_8))
        } catch (cause: Throwable) {
            damaged.renameTo(main)
            throw SessionStorageException("Could not restore session $id", cause)
        }
        emit(recovered)
        recovered
    }

    /** Public MediaStore copies are deliberately outside this deletion boundary. */
    suspend fun requestDelete(id: String, expectedRevision: Long? = null) = ioLocked {
        validateId(id)
        if (hiddenSessionIdsLocked().isNotEmpty()) {
            throw SessionStorageException("Physical deletion is blocked while an unreadable session is preserved")
        }
        // A damaged legacy index may still reference this session's photos or result.
        // Verify and migrate it before writing a deletion intent or removing any file.
        migrateLegacyLocked()
        val current = readDocumentLocked(id) ?: throw SessionMissingException(id)
        if (expectedRevision != null && current.revision != expectedRevision) throw SessionConflictException(id)
        if (!tombstoneFile(id).exists()) {
            val deleted = current.copy(stage = SessionStage.DELETED, revision = current.revision + 1,
                updatedAt = System.currentTimeMillis())
            writeDocumentLocked(deleted)
            writeAtomic(tombstoneFile(id), id.toByteArray(StandardCharsets.UTF_8))
        }
        emit(null, id)
        finishDeleteLocked(current)
    }

    private fun finishDeleteLocked(current: SessionDocument) {
        if (hiddenSessionIdsLocked().isNotEmpty()) {
            throw SessionStorageException("Physical deletion is blocked while an unreadable session is preserved")
        }
        migrateLegacyLocked()
        val id = current.sessionId
        if (!tombstoneFile(id).exists()) {
            writeAtomic(tombstoneFile(id), id.toByteArray(StandardCharsets.UTF_8))
        }
        val referencedElsewhere = readAllLocked().asSequence().filter { it.sessionId != id }
            .flatMap { doc -> (doc.photos.map { it.path } + doc.results.map { it.path }).asSequence() }
            .map { resolveOwnedPath(it).canonicalPath }
            .toSet()
        val publicationIds = publicationIdsLocked(id)
        val unpublishedPaths = publicationIds.mapNotNull { resultId ->
            try { readPublicationLocked(id, resultId)?.path }
            catch (_: SessionCorruptException) {
                // The file name is deterministic even when its journal JSON is damaged.
                if (resultId.matches(Regex("[A-Za-z0-9_-]{1,100}"))) {
                    "results/${id}_${resultId}.jpg"
                } else null
            }
        }
        val ownedPaths = (current.photos.map { it.path } + current.results.map { it.path } +
            unpublishedPaths)
            .map { resolveOwnedPath(it) }
            .distinctBy { it.canonicalPath }
            .filterNot { it.canonicalPath in referencedElsewhere }
        ownedPaths.forEach { file ->
            if (file.exists() && !file.delete()) throw SessionStorageException("Could not delete an owned file")
        }
        val otherCanonicalPaths = referencedElsewhere
        val captureFolder = File(picturesRoot, "captures/$id").canonicalFile
        val captureRoot = File(picturesRoot, "captures").canonicalFile
        if (captureFolder.parentFile != captureRoot) throw SessionStorageException("Invalid capture directory")
        if (captureFolder.exists()) {
            captureFolder.walkBottomUp().forEach { entry ->
                val canonical = entry.canonicalFile
                if (canonical != captureFolder && !canonical.path.startsWith(captureFolder.path + File.separator)) {
                    throw SessionStorageException("Unsafe file in capture directory")
                }
                if (entry.isFile && canonical.path !in otherCanonicalPaths && !entry.delete()) {
                    throw SessionStorageException("Could not delete a capture file")
                }
                if (entry.isDirectory && entry.listFiles().isNullOrEmpty() && !entry.delete()) {
                    throw SessionStorageException("Could not delete an empty capture directory")
                }
            }
        }
        val importFolder = File(picturesRoot, "imports/$id").canonicalFile
        val importRoot = File(picturesRoot, "imports").canonicalFile
        if (importFolder.parentFile != importRoot) throw SessionStorageException("Invalid import directory")
        if (importFolder.exists()) {
            importFolder.walkBottomUp().forEach { entry ->
                val canonical = entry.canonicalFile
                if (canonical != importFolder &&
                    !canonical.path.startsWith(importFolder.path + File.separator)
                ) throw SessionStorageException("Unsafe file in import directory")
                if (entry.isFile && canonical.path !in otherCanonicalPaths && !entry.delete()) {
                    throw SessionStorageException("Could not delete an imported photo")
                }
                if (entry.isDirectory && entry.listFiles().isNullOrEmpty() && !entry.delete()) {
                    throw SessionStorageException("Could not delete an empty import directory")
                }
            }
        }
        val pending = listOf(
            File(context.filesDir, "pending_collage_$id.json"),
            File(context.filesDir, "frame_selection_$id.json"),
        )
        pending.forEach { if (it.exists() && !it.delete()) throw SessionStorageException("Could not delete legacy draft") }
        removeLegacyRowLocked(id)
        val resultsRoot = File(picturesRoot, "results").canonicalFile
        (current.results.map { it.resultId } + publicationIds).distinct().forEach { resultId ->
            validateId(resultId)
            val pendingJpeg = File(resultsRoot, ".${id}_${resultId}.tmp")
            if (pendingJpeg.exists()) {
                if (pendingJpeg.canonicalFile.parentFile != resultsRoot || !pendingJpeg.delete()) {
                    throw SessionStorageException("Could not delete owned pending result")
                }
            }
        }
        publicationIds.forEach { deletePublicationLocked(id, it) }
        deleteSessionDirectoryLocked(quarantineIntentsDir, id)
        deleteSessionDirectoryLocked(quarantinedPublicationsDir, id)
        deleteSessionDirectoryLocked(File(picturesRoot, "recovery_quarantine"), id)
        deleteSessionDirectoryLocked(File(context.filesDir, "import_journals"), id)
        deleteSessionDirectoryLocked(File(context.filesDir, "import_removals"), id)
        val documentsCanonical = documentsDir.canonicalFile
        val corruptName = Regex("${Regex.escape(id)}\\.corrupt\\.[0-9]+")
        documentsDir.listFiles().orEmpty().filter { entry -> corruptName.matches(entry.name) }
            .forEach { entry ->
                if (!entry.isFile || entry.canonicalFile.parentFile != documentsCanonical || !entry.delete()) {
                    throw SessionStorageException("Could not delete owned recovery artifact")
                }
            }
        listOf(lastGoodFile(id), documentFile(id)).forEach { file ->
            File(file.path + ".new").let { pendingWrite ->
                if (pendingWrite.exists() && !pendingWrite.delete()) {
                    throw SessionStorageException("Could not delete unfinished session write")
                }
            }
            if (file.exists() && !file.delete()) throw SessionStorageException("Could not delete session metadata")
            File(file.path + ".bak").let { backup ->
                if (backup.exists() && !backup.delete()) throw SessionStorageException("Could not delete session backup")
            }
        }
    }

    private fun deleteSessionDirectoryLocked(root: File, id: String) {
        validateId(id)
        val directory = File(root, id)
        if (!directory.exists()) return
        val canonicalRoot = root.canonicalFile
        val canonicalDirectory = directory.canonicalFile
        if (canonicalDirectory.parentFile != canonicalRoot) {
            throw SessionStorageException("Invalid recovery artifact directory")
        }
        directory.walkBottomUp().forEach { entry ->
            val canonical = entry.canonicalFile
            if (canonical != canonicalDirectory &&
                !canonical.path.startsWith(canonicalDirectory.path + File.separator)
            ) throw SessionStorageException("Unsafe recovery artifact path")
            if (!entry.delete()) throw SessionStorageException("Could not delete recovery artifact")
        }
    }

    private fun removeLegacyRowLocked(id: String) {
        val legacy = File(context.filesDir, "sessions.json")
        if (!atomicExists(legacy)) return
        val array = try { JSONArray(AtomicFile(legacy).openRead().use { it.readBytes().toString(StandardCharsets.UTF_8) }) }
        catch (cause: Throwable) { throw SessionCorruptException("legacy sessions", cause) }
        val kept = JSONArray()
        var changed = false
        for (index in 0 until array.length()) {
            val row = array.getJSONObject(index)
            if (row.getString("id") == id) changed = true else kept.put(row)
        }
        if (changed) writeAtomic(legacy, kept.toString().toByteArray(StandardCharsets.UTF_8))
    }

    suspend fun migrateLegacy(): List<SessionDocument> = ioLocked { migrateLegacyLocked() }

    fun resolvePhotoPath(photo: PhotoRef): File = resolveOwnedPath(photo.path)

    fun resolveResultPath(result: ResultRecord): File = resolveOwnedPath(result.path)

    private fun publicationDirectory(id: String): File {
        validateId(id)
        return File(resultPublicationsDir, id)
    }

    private fun publicationFile(id: String, resultId: String): File {
        validateId(resultId)
        return File(publicationDirectory(id), "$resultId.json")
    }

    private fun publicationIdsLocked(id: String): List<String> = publicationDirectory(id).listFiles()
        ?.filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".json.bak")) }
        ?.map { it.name.removeSuffix(".bak").removeSuffix(".json") }
        ?.distinct()
        .orEmpty()

    private fun validatePublication(id: String, result: ResultRecord) {
        validateId(id)
        validateId(result.resultId)
        if (result.legacy || result.sourceRevision < 0 || result.width <= 0 || result.height <= 0 ||
            result.createdAt <= 0 || result.path != "results/${id}_${result.resultId}.jpg"
        ) throw SessionStorageException("Invalid result publication record")
        val file = resolveResultPath(result)
        if (file.parentFile?.canonicalFile != File(picturesRoot, "results").canonicalFile) {
            throw SessionStorageException("Result publication is outside the results directory")
        }
    }

    private fun encodePublication(result: ResultRecord): String = JSONObject().apply {
        put("resultId", result.resultId)
        put("sourceRevision", result.sourceRevision)
        put("path", result.path)
        put("width", result.width)
        put("height", result.height)
        put("createdAt", result.createdAt)
    }.toString()

    private fun readPublicationLocked(id: String, resultId: String): ResultRecord? {
        val file = publicationFile(id, resultId)
        if (!atomicExists(file)) return null
        val record = try {
            val json = JSONObject(AtomicFile(file).openRead().bufferedReader().use { it.readText() })
            ResultRecord(
                resultId = json.getString("resultId"),
                sourceRevision = json.getLong("sourceRevision"),
                path = json.getString("path"),
                width = json.getInt("width"),
                height = json.getInt("height"),
                createdAt = json.getLong("createdAt"),
            )
        } catch (cause: Throwable) { throw SessionCorruptException(id, cause) }
        if (record.resultId != resultId) throw SessionCorruptException(id)
        try { validatePublication(id, record) }
        catch (cause: SessionStoreException) { throw SessionCorruptException(id, cause) }
        return record
    }

    private fun deletePublicationLocked(id: String, resultId: String) {
        val file = publicationFile(id, resultId)
        listOf(File(file.path + ".new"), file, File(file.path + ".bak")).forEach { part ->
            if (part.exists() && !part.delete()) throw SessionStorageException("Could not clear result publication")
        }
        publicationDirectory(id).let { directory ->
            if (directory.listFiles().isNullOrEmpty() && directory.exists() && !directory.delete()) {
                throw SessionStorageException("Could not clear empty publication directory")
            }
        }
    }

    /** Rejects traversal and files outside the app-owned pictures directory, including legacy paths. */
    fun resolveOwnedPath(path: String): File {
        val root = picturesRoot.canonicalFile
        val candidate = if (File(path).isAbsolute) File(path) else File(root, path)
        val resolved = candidate.canonicalFile
        if (!resolved.path.startsWith(root.path + File.separator)) {
            throw SessionStorageException("Path is outside app-owned pictures")
        }
        return resolved
    }

    private fun migrateLegacyLocked(): List<SessionDocument> {
        val legacy = File(context.filesDir, "sessions.json")
        val array = try {
            if (atomicExists(legacy)) {
                JSONArray(AtomicFile(legacy).openRead().bufferedReader().use { it.readText() })
            } else JSONArray()
        }
        catch (cause: Throwable) { throw SessionCorruptException("legacy sessions", cause) }
        val migrated = mutableListOf<SessionDocument>()
        val rowIds = mutableSetOf<String>()
        for (index in 0 until array.length()) {
            val row = try { array.getJSONObject(index) }
            catch (cause: Throwable) { throw SessionCorruptException("legacy row $index", cause) }
            val id = try { row.getString("id") }
            catch (cause: Throwable) { throw SessionCorruptException("legacy row $index", cause) }
            validateId(id)
            if (!rowIds.add(id)) throw SessionCorruptException("duplicate legacy session $id")
            if (tombstoneFile(id).exists() || atomicExists(hiddenFile(id)) ||
                atomicExists(documentFile(id))) continue
            val document = try { migrateLegacyRow(row) }
            catch (cause: Throwable) { throw SessionCorruptException(id, cause) }
            validate(document)
            writeDocumentLocked(document)
            emit(document)
            migrated += document
        }
        val captureIds = File(picturesRoot, "captures").listFiles()
            ?.filter { it.isDirectory }?.map { it.name }.orEmpty()
        val pendingIds = context.filesDir.listFiles()?.mapNotNull { file ->
            when {
                file.name.startsWith("pending_collage_") && file.name.endsWith(".json") ->
                    file.name.removePrefix("pending_collage_").removeSuffix(".json")
                file.name.startsWith("frame_selection_") && file.name.endsWith(".json") ->
                    file.name.removePrefix("frame_selection_").removeSuffix(".json")
                else -> null
            }
        }.orEmpty()
        for (id in (captureIds + pendingIds).distinct()) {
            validateId(id)
            if (id in rowIds || tombstoneFile(id).exists() || atomicExists(hiddenFile(id)) ||
                atomicExists(documentFile(id))) continue
            val document = try { migrateUnindexedLegacyDraft(id) }
            catch (cause: Throwable) { throw SessionCorruptException(id, cause) }
            validate(document)
            writeDocumentLocked(document)
            emit(document)
            migrated += document
        }
        return migrated
    }

    private fun migrateUnindexedLegacyDraft(id: String): SessionDocument {
        val photoFiles = File(picturesRoot, "captures/$id").listFiles()
            ?.filter { it.isFile && it.extension.equals("jpg", true) }
            ?.sortedBy { it.name }.orEmpty()
        val photos = photoFiles.mapIndexed { index, file ->
            PhotoRef(
                photoId = UUID.nameUUIDFromBytes("$id:${file.absolutePath}".toByteArray(StandardCharsets.UTF_8)).toString(),
                path = file.absolutePath,
                captureIndex = index,
                legacy = true,
            )
        }
        val pendingFile = File(context.filesDir, "pending_collage_$id.json")
        val frameFile = File(context.filesDir, "frame_selection_$id.json")
        val pending = pendingFile.takeIf { it.exists() }?.let { JSONObject(it.readText()) }
        val frame = frameFile.takeIf { it.exists() }?.let { JSONObject(it.readText()) }
        val timestamp = (photoFiles.map { it.lastModified() } +
            listOfNotNull(pendingFile.takeIf { it.exists() }?.lastModified(), frameFile.takeIf { it.exists() }?.lastModified()))
            .filter { it > 0 }.minOrNull() ?: System.currentTimeMillis()
        return SessionDocument(
            sessionId = id,
            createdAt = timestamp,
            captureCount = 0, // The old unindexed files do not record the intended target count.
            selectedCount = 0,
            stage = SessionStage.NEEDS_RECOVERY,
            photos = photos,
            draft = SessionDraft(
                layoutVersion = 1,
                themeId = legacyText(pending, "themeId") ?: "",
                frameStep = when (legacyText(frame, "type")) {
                    "color", "season", "custom" -> legacyText(frame, "type")!!
                    else -> "choose"
                },
                frameColorId = legacyText(pending, "frameColorId")
                    ?: legacyText(frame, "frameColorId") ?: "white",
                backgroundType = legacyText(pending, "frameBackgroundType") ?: "solid",
                filterId = legacyText(pending, "filterId") ?: "ORIGINAL",
                caption = legacyText(pending, "text") ?: "",
                showDate = pending?.optBoolean("showDate", false) ?: false,
                dateText = sessionDate(timestamp),
                seasonId = legacyText(pending, "seasonId") ?: legacyText(frame, "seasonId"),
                customDesignJson = legacyText(pending, "customDesignJson")
                    ?: legacyText(frame, "customDesignJson"),
            ),
        )
    }

    private fun migrateLegacyRow(row: JSONObject): SessionDocument {
        val id = row.getString("id")
        val selectedPaths = row.getJSONArray("imagePaths").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        }
        val captureFolder = File(picturesRoot, "captures/$id")
        val capturePaths = captureFolder.listFiles()?.filter { it.isFile && it.extension.equals("jpg", true) }
            ?.sortedBy { it.name }?.map { it.absolutePath }.orEmpty()
        val allPaths = (capturePaths + selectedPaths).distinct()
        val photos = allPaths.mapIndexed { index, path ->
            val photoId = UUID.nameUUIDFromBytes("$id:$path".toByteArray(StandardCharsets.UTF_8)).toString()
            PhotoRef(photoId, path, index, legacy = true)
        }
        val selected = selectedPaths.map { path ->
            photos.first { it.path == path }.photoId
        }
        val pending = File(context.filesDir, "pending_collage_$id.json")
            .takeIf { it.exists() }?.let { JSONObject(it.readText()) }
        val frameSelection = File(context.filesDir, "frame_selection_$id.json")
            .takeIf { it.exists() }?.let { JSONObject(it.readText()) }
        val order = pending?.optJSONArray("order")?.let { arr ->
            (0 until arr.length()).map { arr.getInt(it) }
        }
        val orderValid = order == null || (order.size == selected.size && order.sorted() == selected.indices.toList())
        val selectedInOrder = if (orderValid && order != null) order.map { selected[it] } else selected
        val finalPath = legacyText(row, "finalImagePath")
        val result = finalPath?.let { path ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            ResultRecord(
                resultId = UUID.nameUUIDFromBytes("$id:legacy-result:$path".toByteArray()).toString(),
                sourceRevision = 0,
                path = path,
                width = options.outWidth.coerceAtLeast(0),
                height = options.outHeight.coerceAtLeast(0),
                createdAt = row.getLong("createdAt"),
                legacy = true,
            )
        }
        val missing = photos.any { !resolvePhotoPath(it).isFile } ||
            (result != null && !resolveResultPath(result).isFile)
        val createdAt = row.getLong("createdAt")
        val captureCount = row.getInt("captureCount")
        val selectedCount = row.getInt("selectedCount")
        val frameTypeId = inferFrameTypeId(captureCount, selectedCount)
        val legacyFrameId = row.getString("frameId")
        val isLayoutId = FrameLayoutId.entries.any { it.name == legacyFrameId }
        val draft = SessionDraft(
            selectedPhotoIdsInOrder = selectedInOrder,
            layoutId = if (isLayoutId) legacyFrameId else "",
            layoutVersion = 1,
            themeId = legacyText(pending, "themeId") ?: if (isLayoutId) "" else legacyFrameId,
            frameStep = when (legacyText(frameSelection, "type")) {
                "color", "season", "custom" -> legacyText(frameSelection, "type")!!
                else -> "choose"
            },
            frameColorId = legacyText(pending, "frameColorId")
                ?: legacyText(frameSelection, "frameColorId") ?: "white",
            backgroundType = pending?.optString("frameBackgroundType", "solid") ?: "solid",
            filterId = pending?.optString("filterId", "ORIGINAL") ?: "ORIGINAL",
            caption = pending?.optString("text", "") ?: "",
            showDate = pending?.optBoolean("showDate", false) ?: false,
            dateText = sessionDate(createdAt),
            textFontSize = pending?.optDouble("textFontSize", 16.0)?.toFloat() ?: 16f,
            dateFontSize = pending?.optDouble("dateFontSize", 16.0)?.toFloat() ?: 16f,
            captionFontName = legacyText(pending, "captionFontName"),
            captionColorRgb = pending?.takeIf { it.has("captionColorRGB") && !it.isNull("captionColorRGB") }
                ?.getLong("captionColorRGB"),
            seasonId = legacyText(pending, "seasonId")
                ?: legacyText(frameSelection, "seasonId"),
            customDesignJson = legacyText(pending, "customDesignJson")
                ?: legacyText(frameSelection, "customDesignJson"),
        )
        return SessionDocument(
            sessionId = id,
            createdAt = createdAt,
            captureCount = captureCount,
            selectedCount = selectedCount,
            frameTypeId = frameTypeId.orEmpty(),
            stage = when {
                frameTypeId == null || missing || !orderValid || !isLayoutId -> SessionStage.NEEDS_RECOVERY
                result != null -> SessionStage.RESULT
                selected.isNotEmpty() -> SessionStage.DETAIL
                else -> SessionStage.SELECT
            },
            photos = photos,
            draft = draft,
            results = listOfNotNull(result),
        )
    }

    private fun readAllLocked(): List<SessionDocument> = documentsDir.listFiles()
        ?.filter { it.isFile && (it.name.endsWith(".json") || it.name.endsWith(".json.bak")) }
        ?.map { it.name.removeSuffix(".bak").removeSuffix(".json") }
        ?.distinct()
        ?.filterNot { atomicExists(hiddenFile(it)) }
        ?.map { readDocumentLocked(it) ?: error("Missing document") }
        .orEmpty()

    private fun readDocumentLocked(id: String): SessionDocument? {
        val file = documentFile(id)
        if (!atomicExists(file)) return null
        return try {
            decodeValidated(id, AtomicFile(file).openRead().use { it.readBytes() })
        } catch (cause: UnsupportedSessionVersionException) {
            throw cause
        } catch (cause: OccasionCatalogException) {
            // A bundled catalog load failure is retryable app state, not session corruption.
            throw cause
        } catch (cause: Throwable) {
            val backup = lastGoodFile(id)
            if (!atomicExists(backup)) throw SessionCorruptException(id, cause)
            try {
                decodeValidated(id, AtomicFile(backup).openRead().use { it.readBytes() })
                    .copy(stage = SessionStage.NEEDS_RECOVERY)
            } catch (backupCause: UnsupportedSessionVersionException) {
                throw backupCause
            } catch (backupCause: OccasionCatalogException) {
                // A retryable catalog failure does not make the validated backup corrupt.
                throw backupCause
            } catch (backupCause: Throwable) {
                throw SessionCorruptException(id, backupCause)
            }
        }
    }

    private fun decodeValidated(id: String, bytes: ByteArray): SessionDocument {
        val decoded = SessionDocumentCodec.decode(bytes.toString(StandardCharsets.UTF_8))
        val document = if (decoded.stage != SessionStage.NEEDS_RECOVERY && needsOccasionRecovery(decoded)) {
            decoded.copy(stage = SessionStage.NEEDS_RECOVERY)
        } else decoded
        if (document.sessionId != id) throw SessionCorruptException(id)
        validate(document)
        return document
    }

    private fun writeDocumentLocked(document: SessionDocument) {
        val file = documentFile(document.sessionId)
        if (atomicExists(file)) {
            val bytes = try { AtomicFile(file).openRead().use { it.readBytes() } }
            catch (cause: Throwable) { throw SessionCorruptException(document.sessionId, cause) }
            decodeValidated(document.sessionId, bytes)
            writeAtomic(lastGoodFile(document.sessionId), bytes)
        }
        writeAtomic(file, SessionDocumentCodec.encode(document).toByteArray(StandardCharsets.UTF_8))
    }

    private fun writeAtomic(file: File, bytes: ByteArray) {
        file.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw SessionStorageException("Could not create session directory") }
        val atomic = AtomicFile(file)
        val stream = try { atomic.startWrite() }
        catch (cause: Throwable) { throw SessionStorageException("Could not begin session write", cause) }
        try {
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (cause: Throwable) {
            atomic.failWrite(stream)
            throw SessionStorageException("Could not finish session write", cause)
        }
    }

    private fun validate(document: SessionDocument) {
        validateId(document.sessionId)
        if (document.schemaVersion != CURRENT_SESSION_SCHEMA_VERSION) {
            throw UnsupportedSessionVersionException(document.schemaVersion)
        }
        val expectedSelectedCount = when (document.frameTypeId) {
            "2" -> 2
            "4" -> 4
            "6" -> 6
            else -> null
        }
        val expectedCaptureCount = when (document.frameTypeId) {
            "2" -> 4
            "4" -> 8
            "6" -> 10
            else -> null
        }
        val frameContractValid = if (document.stage == SessionStage.NEEDS_RECOVERY) true else when (document.inputSource) {
            InputSource.CAMERA -> expectedSelectedCount == document.selectedCount &&
                expectedCaptureCount == document.captureCount
            InputSource.ALBUM -> expectedSelectedCount == document.selectedCount &&
                document.captureCount == document.selectedCount && document.stage != SessionStage.CAPTURE
        }
        if (document.revision < 0 || document.createdAt < 0 || document.updatedAt < 0 ||
            !frameContractValid ||
            document.captureCount < 0 || document.selectedCount < 0 ||
            document.photos.map { it.photoId }.distinct().size != document.photos.size ||
            document.photos.map { it.captureIndex }.distinct().size != document.photos.size ||
            document.results.map { it.resultId }.distinct().size != document.results.size ||
            document.exportOperations.map { it.operationId }.distinct().size != document.exportOperations.size
        ) throw SessionCorruptException(document.sessionId)
        document.photos.forEach { photo ->
            validateId(photo.photoId)
            if (photo.captureIndex < 0 || photo.path.isBlank()) throw SessionCorruptException(document.sessionId)
            val file = resolvePhotoPath(photo)
            if (!photo.legacy) {
                val expectedParent = when (document.inputSource) {
                    InputSource.CAMERA -> File(picturesRoot, "captures/${document.sessionId}")
                    InputSource.ALBUM -> File(picturesRoot, "imports/${document.sessionId}")
                }.canonicalFile
                if (file.parentFile?.canonicalFile != expectedParent) {
                    throw SessionCorruptException(document.sessionId)
                }
            }
        }
        document.results.forEach { result ->
            validateId(result.resultId)
            if (result.path.isBlank() || result.width < 0 || result.height < 0) throw SessionCorruptException(document.sessionId)
            resolveResultPath(result)
        }
        val resultIds = document.results.map { it.resultId }.toSet()
        document.exportOperations.forEach { operation ->
            validateId(operation.operationId)
            if (operation.resultId !in resultIds ||
                operation.displayName != "Pocket4Cut_${operation.resultId}_${operation.operationId}.jpg") {
                throw SessionCorruptException(document.sessionId)
            }
        }
        val knownPhotos = document.photos.map { it.photoId }.toSet()
        val collectionCountsValid = document.stage == SessionStage.NEEDS_RECOVERY ||
            (document.photos.size <= document.captureCount &&
                document.draft.selectedPhotoIdsInOrder.size <= document.selectedCount)
        if (document.draft.selectedPhotoIdsInOrder.distinct().size != document.draft.selectedPhotoIdsInOrder.size ||
            !collectionCountsValid ||
            !knownPhotos.containsAll(document.draft.selectedPhotoIdsInOrder) ||
            !knownPhotos.containsAll(document.draft.adjustmentsByPhotoId.keys) ||
            document.draft.layoutVersion < 1 || document.draft.frameStep !in
                setOf("choose", "color", "season", "custom", "occasion") ||
            document.draft.backgroundType !in setOf("solid", "season", "occasion") ||
            (document.stage != SessionStage.NEEDS_RECOVERY && needsOccasionRecovery(document)) ||
            !document.draft.textFontSize.isFinite() ||
            !document.draft.dateFontSize.isFinite() || document.draft.adjustmentsByPhotoId.values.any {
                !it.brightness.isFinite() || !it.contrast.isFinite() || !it.saturation.isFinite() ||
                    !it.crop.focusX.isFinite() || it.crop.focusX !in 0f..1f ||
                    !it.crop.focusY.isFinite() || it.crop.focusY !in 0f..1f ||
                    !it.crop.zoom.isFinite() || it.crop.zoom !in 1f..4f
            }
        ) throw SessionCorruptException(document.sessionId)
    }

    /**
     * Occasion data is never guessed or silently replaced. A document read with a missing,
     * inconsistent, newer, or unknown occasion selection is surfaced as NEEDS_RECOVERY.
     */
    private fun needsOccasionRecovery(document: SessionDocument): Boolean {
        val draft = document.draft
        val knownIds = if (draft.backgroundType == "occasion" || draft.occasionThemeId != null ||
            draft.occasionDesignVersion != null
        ) knownOccasionThemeIds else emptySet()
        return OccasionSelectionContract.evaluate(
            backgroundType = draft.backgroundType,
            themeId = draft.occasionThemeId,
            designVersion = draft.occasionDesignVersion,
            knownThemeIds = knownIds,
            seasonId = draft.seasonId,
            customDesignJson = draft.customDesignJson,
        ) == OccasionSelectionState.NEEDS_RECOVERY
    }

    private fun documentFile(id: String): File { validateId(id); return File(documentsDir, "$id.json") }
    private fun atomicExists(file: File): Boolean = file.exists() || File(file.path + ".bak").exists()
    private fun lastGoodFile(id: String): File { validateId(id); return File(documentsDir, "$id.json.lastgood") }
    private fun tombstoneFile(id: String): File { validateId(id); return File(tombstonesDir, "$id.deleted") }
    private fun hiddenFile(id: String): File { validateId(id); return File(hiddenDir, "$id.hidden") }
    private fun validateId(id: String) {
        if (!id.matches(Regex("[A-Za-z0-9_-]{1,100}"))) throw SessionStorageException("Invalid session identifier")
    }
    private fun sessionDate(millis: Long): String =
        SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(millis))
    private fun legacyText(json: JSONObject?, key: String): String? =
        json?.takeIf { it.has(key) && !it.isNull(key) }
            ?.optString(key)?.takeIf { it.isNotBlank() }
    private fun isJpeg(file: File): Boolean {
        if (file.length() < 4) return false
        val markers = try {
            RandomAccessFile(file, "r").use { input ->
                val start = input.readUnsignedShort()
                input.seek(input.length() - 2)
                start == 0xFFD8 && input.readUnsignedShort() == 0xFFD9
            }
        } catch (_: IOException) {
            false
        }
        if (!markers) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth > 0 && options.outHeight > 0
    }
    private fun resultJpegMatchesRecord(file: File, record: ResultRecord): Boolean {
        if (!isJpeg(file)) return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth == record.width && options.outHeight == record.height
    }
    private fun key(id: String): String = "${context.filesDir.absolutePath}:$id"
    private fun emit(document: SessionDocument?) {
        val id = document?.sessionId ?: return
        emit(document, id)
    }
    private fun emit(document: SessionDocument?, id: String) { flows.getOrPut(key(id)) { MutableStateFlow(null) }.value = document }
    private suspend fun <T> ioLocked(block: () -> T): T = withContext(Dispatchers.IO) { processMutex.withLock { block() } }

    companion object {
        private val processMutex = Mutex()
        private val flows = ConcurrentHashMap<String, MutableStateFlow<SessionDocument?>>()
    }
}
