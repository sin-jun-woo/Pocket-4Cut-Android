package com.pocket4cut

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.local.SessionConflictException
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.data.local.SessionStorageException
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.ExportOperation
import com.pocket4cut.domain.model.ExportStatus
import com.pocket4cut.domain.model.ResultRecord
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionStage
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SessionDocumentRepositoryInstrumentedTest {
    private lateinit var testRoot: File
    private lateinit var context: Context
    private lateinit var repository: SessionDocumentRepository

    @Before fun setUp() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        testRoot = File(app.cacheDir, "session-store-test-${UUID.randomUUID()}").apply { mkdirs() }
        context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(testRoot, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(testRoot, "external/$type").apply { mkdirs() }
        }
        repository = SessionDocumentRepository(context)
    }

    @After fun tearDown() {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.canonicalFile
        assertTrue(testRoot.canonicalPath.startsWith(cache.path + File.separator))
        testRoot.deleteRecursively()
    }

    @Test fun revisionRejectsStaleWritesAndOriginalIsUnchanged() = runBlocking {
        val id = UUID.randomUUID().toString()
        val original = captureFile(id, "cap_00.jpg")
        val bytesBefore = original.readBytes()
        val created = repository.create(SessionDocument(
            sessionId = id, createdAt = 1_700_000_000_000L, captureCount = 4, selectedCount = 2,
        ))
        val photo = PhotoRef(UUID.randomUUID().toString(), "captures/$id/cap_00.jpg", 0)
        val captured = repository.appendCapture(id, created.revision, photo)
        assertEquals(listOf(photo), captured.photos)
        assertEquals(bytesBefore.toList(), original.readBytes().toList())
        val edited = repository.update(id, captured.revision) {
            it.copy(stage = SessionStage.EDIT,
                draft = it.draft.copy(selectedPhotoIdsInOrder = listOf(photo.photoId), caption = "test"))
        }
        assertEquals("test", repository.getById(id)?.draft?.caption)
        assertEquals(edited.revision, repository.getById(id)?.revision)
        try {
            repository.update(id, captured.revision) { it.copy(draft = it.draft.copy(caption = "stale")) }
            throw AssertionError("Stale revision was accepted")
        } catch (_: SessionConflictException) { }
    }

    @Test fun truncatedCaptureIsNotPublishedInSession() = runBlocking {
        val id = UUID.randomUUID().toString()
        val jpeg = captureFile(id, "cap_00.jpg")
        val bytes = jpeg.readBytes()
        jpeg.writeBytes(bytes.copyOf(bytes.size - 2))
        val created = repository.create(SessionDocument(
            sessionId = id, createdAt = 1_700_000_000_000L, captureCount = 4, selectedCount = 2,
        ))
        try {
            repository.appendCapture(id, created.revision,
                PhotoRef(UUID.randomUUID().toString(), "captures/$id/cap_00.jpg", 0))
            throw AssertionError("Truncated JPEG was accepted")
        } catch (_: SessionStorageException) { }
        assertTrue(repository.getById(id)!!.photos.isEmpty())
    }

    @Test fun corruptPrimaryNeedsRecoveryAndRestoresLastGoodRevision() = runBlocking {
        val id = UUID.randomUUID().toString()
        val created = repository.create(SessionDocument(
            sessionId = id, createdAt = 1_700_000_000_000L, captureCount = 8, selectedCount = 4,
        ))
        repository.update(id, created.revision) { it.copy(draft = it.draft.copy(caption = "new")) }
        File(context.filesDir, "session_documents/$id.json").writeText("{invalid")
        assertEquals(SessionStage.NEEDS_RECOVERY, repository.getById(id)?.stage)
        val recovered = repository.recover(id)
        assertNotNull(recovered)
        assertEquals(created.revision, recovered?.revision)
        assertEquals(SessionStage.CAPTURE, repository.getById(id)?.stage)
        assertTrue(File(context.filesDir, "session_documents").listFiles().orEmpty()
            .any { it.name.startsWith("$id.corrupt.") })
    }

    @Test fun galleryScanExposesUnreadableSessionWithoutHidingHealthySessions() = runBlocking {
        val healthyId = UUID.randomUUID().toString()
        val damagedId = UUID.randomUUID().toString()
        val healthyPhoto = captureFile(healthyId, "cap_00.jpg")
        val healthyBytes = healthyPhoto.readBytes()
        val healthy = repository.create(SessionDocument(
            sessionId = healthyId, createdAt = 1_700_000_000_000L,
            captureCount = 4, selectedCount = 2,
        ))
        repository.appendCapture(healthyId, healthy.revision,
            PhotoRef(UUID.randomUUID().toString(), "captures/$healthyId/cap_00.jpg", 0))
        val damaged = repository.create(SessionDocument(
            sessionId = damagedId, createdAt = 1_700_000_000_001L,
            captureCount = 4, selectedCount = 2,
        ))
        repository.update(damagedId, damaged.revision) { it.copy(draft = it.draft.copy(caption = "latest")) }
        val directory = File(context.filesDir, "session_documents")
        File(directory, "$damagedId.json").writeText("{damaged")
        File(directory, "$damagedId.json.lastgood").writeText("{also damaged")

        val scan = SessionDocumentRepository(context).scanForGallery()
        assertEquals(listOf(healthyId), scan.documents.map { it.sessionId })
        assertEquals(listOf(damagedId), scan.unreadableSessionIds)
        assertTrue(File(directory, "$damagedId.json").exists())
        assertTrue(File(directory, "$damagedId.json.lastgood").exists())

        val damagedBytes = File(directory, "$damagedId.json").readBytes()
        repository.hideUnreadableSession(damagedId)
        val afterHide = SessionDocumentRepository(context)
        assertEquals(listOf(healthyId), afterHide.list().map { it.sessionId })
        assertTrue(afterHide.scanForGallery().unreadableSessionIds.isEmpty())
        assertEquals(null, afterHide.getById(damagedId))
        assertEquals(damagedBytes.toList(), File(directory, "$damagedId.json").readBytes().toList())
        assertEquals(healthyBytes.toList(), healthyPhoto.readBytes().toList())
        try {
            afterHide.requestDelete(healthyId)
            throw AssertionError("Deletion proceeded while an unreadable record may reference the photo")
        } catch (_: SessionStorageException) { }
        assertEquals(healthyBytes.toList(), healthyPhoto.readBytes().toList())

        assertEquals(listOf(damagedId), afterHide.listHiddenSessionIds())
        afterHide.unhideSession(damagedId)
        assertTrue(afterHide.listHiddenSessionIds().isEmpty())
        assertEquals(listOf(damagedId), afterHide.scanForGallery().unreadableSessionIds)
        assertEquals(damagedBytes.toList(), File(directory, "$damagedId.json").readBytes().toList())
    }

    @Test fun deletingSessionClearsOnlyKnownPendingResultFile() = runBlocking {
        val id = UUID.randomUUID().toString()
        val otherId = UUID.randomUUID().toString()
        val resultId = UUID.randomUUID().toString()
        val created = repository.create(SessionDocument(
            sessionId = id, createdAt = 1_700_000_000_000L,
            captureCount = 4, selectedCount = 2,
        ))
        repository.prepareResultPublication(id, created.revision,
            ResultRecord(resultId, created.revision, "results/${id}_${resultId}.jpg", 8, 8, 1L))
        val resultRoot = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "Pocket4Cut/results").apply { mkdirs() }
        val ownPending = File(resultRoot, ".${id}_${resultId}.tmp").apply {
            writeBytes(byteArrayOf(0x12, 0x34))
        }
        val otherPending = File(resultRoot, ".${otherId}_${resultId}.tmp").apply {
            writeBytes(byteArrayOf(0x56, 0x78))
        }

        repository.requestDelete(id)
        assertFalse(ownPending.exists())
        assertTrue(otherPending.exists())
        assertEquals(null, repository.getById(id))
    }

    @Test fun deletingSessionRemovesOnlyItsRecoveryArtifacts() = runBlocking {
        val id = UUID.randomUUID().toString()
        val otherId = UUID.randomUUID().toString()
        val created = repository.create(SessionDocument(
            sessionId = id, createdAt = 1_700_000_000_000L, captureCount = 4, selectedCount = 2,
        ))
        repository.update(id, created.revision) { it.copy(draft = it.draft.copy(caption = "latest")) }
        val dir = File(context.filesDir, "session_documents")
        File(dir, "$id.json").writeText("{invalid")
        repository.recover(id)
        val ownArtifacts = dir.listFiles().orEmpty().filter { it.name.startsWith("$id.corrupt.") }
        assertTrue(ownArtifacts.isNotEmpty())
        val unrelatedArtifact = File(dir, "$otherId.corrupt.123456").apply { writeText("other") }

        repository.requestDelete(id)
        assertTrue(ownArtifacts.all { !it.exists() })
        assertTrue(unrelatedArtifact.exists())
        assertEquals(null, repository.getById(id))
    }

    @Test fun migrationTreatsLegacyImagePathsAsAlreadySelectedAndDoesNotRepeat() = runBlocking {
        val id = UUID.randomUUID().toString()
        val first = captureFile(id, "cap_00.jpg")
        val second = captureFile(id, "cap_01.jpg")
        val firstBytes = first.readBytes()
        val secondBytes = second.readBytes()
        val row = JSONObject()
            .put("id", id).put("captureCount", 4).put("selectedCount", 2)
            .put("imagePaths", JSONArray(listOf(first.absolutePath, second.absolutePath)))
            .put("selectedIndexes", JSONArray(listOf(3, 1)))
            .put("frameId", "TWO_BASIC")
            .put("finalImagePath", "")
            .put("createdAt", 1_700_000_000_000L)
        File(context.filesDir, "sessions.json").writeText(JSONArray().put(row).toString())
        File(context.filesDir, "pending_collage_$id.json").writeText(
            JSONObject().put("order", JSONArray(listOf(1, 0))).put("text", "before").toString(),
        )
        val migrated = repository.migrateLegacy().single()
        assertEquals(listOf(second.absolutePath, first.absolutePath),
            migrated.draft.selectedPhotoIdsInOrder.map { photoId ->
                migrated.photos.first { it.photoId == photoId }.path
            })
        assertEquals("before", migrated.draft.caption)
        assertTrue(repository.migrateLegacy().isEmpty())
        assertEquals(firstBytes.toList(), first.readBytes().toList())
        assertEquals(secondBytes.toList(), second.readBytes().toList())
    }

    @Test fun deletedLegacySessionDoesNotReappearAndOnlyOwnedFileIsRemoved() = runBlocking {
        val id = UUID.randomUUID().toString()
        val photo = captureFile(id, "cap_00.jpg")
        val row = JSONObject()
            .put("id", id).put("captureCount", 4).put("selectedCount", 2)
            .put("imagePaths", JSONArray(listOf(photo.absolutePath)))
            .put("selectedIndexes", JSONArray(listOf(0)))
            .put("frameId", "TWO_BASIC").put("finalImagePath", "")
            .put("createdAt", 1_700_000_000_000L)
        File(context.filesDir, "sessions.json").writeText(JSONArray().put(row).toString())
        repository.migrateLegacy()
        repository.requestDelete(id)
        assertFalse(photo.exists())
        assertTrue(repository.migrateLegacy().isEmpty())
        assertEquals(null, repository.getById(id))
        assertFalse(repository.list().any { it.sessionId == id })
    }

    @Test fun deletionResumesFromDurableTombstoneAfterProcessRestart() = runBlocking {
        val id = UUID.randomUUID().toString()
        val photo = captureFile(id, "cap_00.jpg")
        val repositoryBeforeRestart = SessionDocumentRepository(context)
        val created = repositoryBeforeRestart.create(SessionDocument(
            sessionId = id, createdAt = 1_700_000_000_000L,
            captureCount = 4, selectedCount = 2,
        ))
        repositoryBeforeRestart.appendCapture(id, created.revision,
            PhotoRef(UUID.randomUUID().toString(), "captures/$id/cap_00.jpg", 0))

        // Fault injection: process death after a delete intent, before any owned file is removed.
        val tombstone = File(context.filesDir, "session_documents/tombstones/$id.deleted")
        tombstone.parentFile!!.mkdirs()
        tombstone.writeText(id)
        val restarted = SessionDocumentRepository(context)
        assertFalse(restarted.list().any { it.sessionId == id })
        assertFalse(photo.exists())
        assertEquals(null, restarted.getById(id))
        assertFalse(SessionDocumentRepository(context).list().any { it.sessionId == id })
    }

    @Test fun deletionPreservesFileReferencedByAnotherSessionAcrossPathFormats() = runBlocking {
        val firstId = UUID.randomUUID().toString()
        val secondId = UUID.randomUUID().toString()
        val shared = captureFile(firstId, "shared.jpg")
        repository.create(SessionDocument(
            sessionId = firstId, createdAt = 1_700_000_000_000L,
            captureCount = 4, selectedCount = 2,
            photos = listOf(PhotoRef(UUID.randomUUID().toString(), shared.absolutePath, 0, legacy = true)),
        ))
        repository.create(SessionDocument(
            sessionId = secondId, createdAt = 1_700_000_000_000L,
            captureCount = 4, selectedCount = 2,
            photos = listOf(PhotoRef(UUID.randomUUID().toString(),
                "captures/$firstId/shared.jpg", 0, legacy = true)),
        ))
        repository.requestDelete(firstId)
        assertTrue(shared.exists())
        repository.requestDelete(secondId)
        assertFalse(shared.exists())
    }

    @Test fun exportCopyFlagSurvivesDocumentReload() = runBlocking {
        val id = UUID.randomUUID().toString()
        val resultId = UUID.randomUUID().toString()
        val operationId = UUID.randomUUID().toString()
        val source = captureFile(id, "source.jpg")
        val resultFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "Pocket4Cut/results/$resultId.jpg")
        resultFile.parentFile!!.mkdirs()
        source.copyTo(resultFile)
        val created = repository.create(SessionDocument(
            sessionId = id, createdAt = 1_700_000_000_000L, captureCount = 4, selectedCount = 2,
            stage = SessionStage.RESULT,
            results = listOf(ResultRecord(resultId, 0, "results/$resultId.jpg", 4, 4, 1L)),
        ))
        repository.update(id, created.revision) {
            it.copy(exportOperations = listOf(ExportOperation(
                operationId = operationId, resultId = resultId,
                status = ExportStatus.PREPARED,
                displayName = "Pocket4Cut_${resultId}_${operationId}.jpg",
                createdAt = 1L, isCopy = true,
            )))
        }
        assertTrue(SessionDocumentRepository(context).getById(id)!!.exportOperations.single().isCopy)
    }

    private fun captureFile(id: String, name: String): File {
        val pictures = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val file = File(pictures, "Pocket4Cut/captures/$id/$name")
        file.parentFile!!.mkdirs()
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
        bitmap.recycle()
        return file
    }
}
