package com.pocket4cut

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.importing.PhotoImportFailureReason
import com.pocket4cut.data.importing.PhotoImportRepository
import com.pocket4cut.data.local.SessionCorruptException
import com.pocket4cut.data.local.SessionDocumentCodec
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.data.local.UnsupportedSessionVersionException
import com.pocket4cut.domain.model.CURRENT_SESSION_SCHEMA_VERSION
import com.pocket4cut.domain.model.InputSource
import com.pocket4cut.domain.model.PhotoAdjustments
import com.pocket4cut.domain.model.PhotoCrop
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.domain.model.SessionStage
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PhotoImportRepositoryInstrumentedTest {
    private lateinit var testRoot: File
    private lateinit var context: Context
    private lateinit var sessions: SessionDocumentRepository
    private lateinit var imports: PhotoImportRepository

    @Before fun setUp() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        testRoot = File(app.cacheDir, "photo-import-test-${UUID.randomUUID()}").apply { mkdirs() }
        context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(testRoot, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(testRoot, "external/$type").apply { mkdirs() }
        }
        sessions = SessionDocumentRepository(context)
        imports = PhotoImportRepository(context, sessions)
    }

    @After fun tearDown() {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.canonicalFile
        assertTrue(testRoot.canonicalPath.startsWith(cache.path + File.separator))
        testRoot.deleteRecursively()
    }

    @Test fun schemaV1IsStrictlyUpgradedAndWrittenAsCurrentSchemaOnNextUpdate() = runBlocking {
        val id = UUID.randomUUID().toString()
        val photoId = UUID.randomUUID().toString()
        val document = SessionDocument(
            sessionId = id,
            createdAt = 1_700_000_000_000L,
            captureCount = 4,
            selectedCount = 2,
            photos = listOf(PhotoRef(photoId, "captures/$id/cap_01.jpg", 0)),
            draft = SessionDraft(
                selectedPhotoIdsInOrder = listOf(photoId),
                adjustmentsByPhotoId = mapOf(photoId to PhotoAdjustments()),
            ),
        )
        val json = JSONObject(SessionDocumentCodec.encode(document)).apply {
            put("schemaVersion", 1)
            remove("frameTypeId")
            remove("inputSource")
            getJSONObject("draft").getJSONObject("adjustmentsByPhotoId")
                .getJSONObject(photoId).remove("crop")
        }
        val file = File(context.filesDir, "session_documents/$id.json")
        file.parentFile!!.mkdirs()
        file.writeText(json.toString())

        val migrated = sessions.getById(id)!!
        assertEquals(CURRENT_SESSION_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals("2", migrated.frameTypeId)
        assertEquals(InputSource.CAMERA, migrated.inputSource)
        assertEquals(PhotoCrop(), migrated.draft.adjustmentsByPhotoId.getValue(photoId).crop)
        sessions.update(id, migrated.revision) { it.copy(draft = it.draft.copy(caption = "migrated")) }
        val persisted = JSONObject(file.readText())
        assertEquals(CURRENT_SESSION_SCHEMA_VERSION, persisted.getInt("schemaVersion"))
        assertEquals("2", persisted.getString("frameTypeId"))
    }

    @Test fun invalidV1CountsBecomeRecoveryInsteadOfGuessingFourCut() = runBlocking {
        val id = UUID.randomUUID().toString()
        val json = JSONObject(SessionDocumentCodec.encode(SessionDocument(
            sessionId = id,
            createdAt = 1_700_000_000_000L,
            captureCount = 4,
            selectedCount = 2,
        ))).apply {
            put("schemaVersion", 1)
            put("captureCount", 5)
            remove("frameTypeId")
            remove("inputSource")
        }
        val file = File(context.filesDir, "session_documents/$id.json")
        file.parentFile!!.mkdirs()
        file.writeText(json.toString())
        val migrated = sessions.getById(id)!!
        assertEquals(SessionStage.NEEDS_RECOVERY, migrated.stage)
        assertEquals("", migrated.frameTypeId)
    }

    @Test fun schemaV1FourAndSixCutPairsMigrateWithoutChangingOrder() = runBlocking {
        listOf(Triple(8, 4, "4"), Triple(10, 6, "6")).forEach { (captureCount, selectedCount, frameTypeId) ->
            val id = UUID.randomUUID().toString()
            val photoIds = List(selectedCount) { UUID.randomUUID().toString() }
            val document = SessionDocument(
                sessionId = id,
                createdAt = 1_700_000_000_000L,
                captureCount = captureCount,
                selectedCount = selectedCount,
                photos = photoIds.mapIndexed { index, photoId ->
                    PhotoRef(photoId, "captures/$id/cap_${index + 1}.jpg", index)
                },
                draft = SessionDraft(selectedPhotoIdsInOrder = photoIds.reversed()),
            )
            val json = JSONObject(SessionDocumentCodec.encode(document)).apply {
                put("schemaVersion", 1)
                remove("frameTypeId")
                remove("inputSource")
            }
            val file = File(context.filesDir, "session_documents/$id.json")
            file.parentFile!!.mkdirs()
            file.writeText(json.toString())
            val migrated = sessions.getById(id)!!
            assertEquals(frameTypeId, migrated.frameTypeId)
            assertEquals(photoIds.reversed(), migrated.draft.selectedPhotoIdsInOrder)
            assertEquals(InputSource.CAMERA, migrated.inputSource)
        }
    }

    @Test fun futureSchemaIsRejectedWithoutRewritingItsDocument() = runBlocking {
        val id = UUID.randomUUID().toString()
        val json = JSONObject(SessionDocumentCodec.encode(SessionDocument(
            sessionId = id,
            createdAt = 1_700_000_000_000L,
            captureCount = 4,
            selectedCount = 2,
        ))).apply { put("schemaVersion", CURRENT_SESSION_SCHEMA_VERSION + 1) }
        val file = File(context.filesDir, "session_documents/$id.json")
        file.parentFile!!.mkdirs()
        val before = json.toString()
        file.writeText(before)
        try {
            sessions.getById(id)
            throw AssertionError("Future schema was accepted")
        } catch (error: UnsupportedSessionVersionException) {
            assertEquals(CURRENT_SESSION_SCHEMA_VERSION + 1, error.version)
        }
        assertEquals(before, file.readText())
    }

    @Test fun invalidCropIsRejectedAsCorruptSession() = runBlocking {
        val id = UUID.randomUUID().toString()
        val photoId = UUID.randomUUID().toString()
        val json = SessionDocumentCodec.encode(SessionDocument(
            sessionId = id,
            createdAt = 1_700_000_000_000L,
            captureCount = 4,
            selectedCount = 2,
            photos = listOf(PhotoRef(photoId, "captures/$id/cap_01.jpg", 0)),
            draft = SessionDraft(adjustmentsByPhotoId = mapOf(
                photoId to PhotoAdjustments(crop = PhotoCrop(0.5f, 0.5f, 4.01f)),
            )),
        ))
        val file = File(context.filesDir, "session_documents/$id.json")
        file.parentFile!!.mkdirs()
        file.writeText(json)
        try {
            sessions.getById(id)
            throw AssertionError("Out-of-range crop was accepted")
        } catch (_: SessionCorruptException) { }
    }

    @Test fun importPreservesExternalBytesReplaysWithoutDuplicatesAndRemovesOnlyCopy() = runBlocking {
        val id = UUID.randomUUID().toString()
        val first = jpeg("first.jpg", android.graphics.Color.RED)
        val second = jpeg("second.jpg", android.graphics.Color.BLUE)
        val firstHash = sha256(first)
        val secondHash = sha256(second)

        val result = imports.importUris(id, "2", listOf(Uri.fromFile(first), Uri.fromFile(second)))
        assertTrue(result.failures.isEmpty())
        assertEquals(2, result.importedPhotoIds.size)
        val document = result.session!!
        assertEquals(InputSource.ALBUM, document.inputSource)
        assertEquals(SessionStage.IMPORT, document.stage)
        assertEquals("2", document.frameTypeId)
        assertEquals(document.photos.map { it.photoId }, document.draft.selectedPhotoIdsInOrder)
        assertEquals(firstHash, sha256(first))
        assertEquals(secondHash, sha256(second))
        document.photos.forEach { photo -> assertTrue(sessions.resolvePhotoPath(photo).isFile) }

        val replayed = PhotoImportRepository(context, SessionDocumentRepository(context)).recover(id)
        assertTrue(replayed.failures.isEmpty())
        assertTrue(replayed.recoveredPhotoIds.isEmpty())
        assertEquals(2, replayed.session!!.photos.size)

        val removedPhoto = replayed.session.photos.first()
        val removedFile = sessions.resolvePhotoPath(removedPhoto)
        val afterRemoval = imports.removePhoto(id, removedPhoto.photoId, replayed.session.revision)
        assertEquals(1, afterRemoval.photos.size)
        assertFalse(removedFile.exists())
        assertTrue(first.exists())
        assertTrue(second.exists())
        assertEquals(firstHash, sha256(first))
        assertEquals(secondHash, sha256(second))
    }

    @Test fun cropRemainsAttachedToPhotoIdAfterReorderAndRepositoryRecreation() = runBlocking {
        val id = UUID.randomUUID().toString()
        val result = imports.importUris(id, "2", listOf(
            Uri.fromFile(jpeg("crop-first.jpg", android.graphics.Color.RED)),
            Uri.fromFile(jpeg("crop-second.jpg", android.graphics.Color.BLUE)),
        ))
        assertTrue(result.failures.isEmpty())
        val original = result.session!!
        val firstId = original.photos[0].photoId
        val secondId = original.photos[1].photoId
        val firstCrop = PhotoCrop(focusX = 0.2f, focusY = 0.35f, zoom = 1.75f)
        val secondCrop = PhotoCrop(focusX = 0.8f, focusY = 0.65f, zoom = 3.25f)

        sessions.update(id, original.revision) { current ->
            current.copy(draft = current.draft.copy(
                selectedPhotoIdsInOrder = listOf(secondId, firstId),
                adjustmentsByPhotoId = mapOf(
                    firstId to PhotoAdjustments(crop = firstCrop),
                    secondId to PhotoAdjustments(crop = secondCrop),
                ),
            ))
        }

        val restored = SessionDocumentRepository(context).getById(id)!!
        assertEquals(listOf(secondId, firstId), restored.draft.selectedPhotoIdsInOrder)
        assertEquals(firstCrop, restored.draft.adjustmentsByPhotoId.getValue(firstId).crop)
        assertEquals(secondCrop, restored.draft.adjustmentsByPhotoId.getValue(secondId).crop)
    }

    @Test fun preparedJournalWithAlreadyRenamedFileRecoversExactlyOnce() = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        val photoId = UUID.randomUUID().toString()
        val source = jpeg("rename-window-source.jpg", android.graphics.Color.CYAN)
        val finalFile = importFile(sessionId, "$photoId.jpg")
        source.copyTo(finalFile)
        writeImportJournal(
            sessionId = sessionId,
            photoId = photoId,
            source = source,
            status = "PREPARED",
            finalPath = null,
            byteCount = null,
            hash = null,
        )

        val firstRecovery = PhotoImportRepository(
            context,
            SessionDocumentRepository(context),
        ).recoverAll().single { it.session?.sessionId == sessionId }
        assertTrue(firstRecovery.failures.isEmpty())
        assertEquals(listOf(photoId), firstRecovery.recoveredPhotoIds)
        assertEquals(listOf(photoId), firstRecovery.session!!.photos.map { it.photoId })
        assertEquals(sha256(source), sha256(finalFile))

        val secondRecovery = PhotoImportRepository(
            context,
            SessionDocumentRepository(context),
        ).recoverAll().single { it.session?.sessionId == sessionId }
        assertTrue(secondRecovery.failures.isEmpty())
        assertTrue(secondRecovery.recoveredPhotoIds.isEmpty())
        assertEquals(1, secondRecovery.session!!.photos.count { it.photoId == photoId })
    }

    @Test fun filePublishedJournalWithoutSessionCommitRecoversExactlyOnce() = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        val photoId = UUID.randomUUID().toString()
        val source = jpeg("published-window-source.jpg", android.graphics.Color.MAGENTA)
        val finalFile = importFile(sessionId, "$photoId.jpg")
        source.copyTo(finalFile)
        writeImportJournal(
            sessionId = sessionId,
            photoId = photoId,
            source = source,
            status = "FILE_PUBLISHED",
            finalPath = "imports/$sessionId/${finalFile.name}",
            byteCount = finalFile.length(),
            hash = sha256(finalFile),
        )

        val firstRecovery = imports.recover(sessionId)
        assertTrue(firstRecovery.failures.isEmpty())
        assertEquals(listOf(photoId), firstRecovery.recoveredPhotoIds)
        assertEquals(1, firstRecovery.session!!.photos.count { it.photoId == photoId })

        val secondRecovery = imports.recover(sessionId)
        assertTrue(secondRecovery.failures.isEmpty())
        assertTrue(secondRecovery.recoveredPhotoIds.isEmpty())
        assertEquals(1, secondRecovery.session!!.photos.count { it.photoId == photoId })
    }

    @Test fun removalIntentAfterReferenceRemovalDeletesOnlyItsOwnedImportCopy() = runBlocking {
        val firstSource = jpeg("removal-target-source.jpg", android.graphics.Color.YELLOW)
        val otherSource = jpeg("removal-other-source.jpg", android.graphics.Color.GREEN)
        val firstSessionId = UUID.randomUUID().toString()
        val otherSessionId = UUID.randomUUID().toString()
        val imported = imports.importUris(
            firstSessionId,
            "2",
            listOf(Uri.fromFile(firstSource)),
        ).session!!
        val other = imports.importUris(
            otherSessionId,
            "2",
            listOf(Uri.fromFile(otherSource)),
        ).session!!
        val removedPhoto = imported.photos.single()
        val removedCopy = sessions.resolvePhotoPath(removedPhoto)
        val otherCopy = sessions.resolvePhotoPath(other.photos.single())
        val otherCopyHash = sha256(otherCopy)
        val firstSourceHash = sha256(firstSource)
        val otherSourceHash = sha256(otherSource)

        val withoutReference = sessions.removeImportedPhoto(
            firstSessionId,
            imported.revision,
            removedPhoto.photoId,
        )
        assertTrue(withoutReference.photos.isEmpty())
        assertTrue(removedCopy.isFile)
        writeRemovalJournal(firstSessionId, removedPhoto.photoId, removedPhoto.path)

        val recovered = PhotoImportRepository(
            context,
            SessionDocumentRepository(context),
        ).recover(firstSessionId)
        assertTrue(recovered.failures.isEmpty())
        assertTrue(recovered.session!!.photos.isEmpty())
        assertFalse(removedCopy.exists())
        assertTrue(otherCopy.isFile)
        assertEquals(otherCopyHash, sha256(otherCopy))
        assertEquals(firstSourceHash, sha256(firstSource))
        assertEquals(otherSourceHash, sha256(otherSource))
        assertEquals(otherSessionId, sessions.getById(otherSessionId)?.sessionId)
    }

    @Test fun mismatchedRemovalJournalCannotDeleteAnotherSessionsImport() = runBlocking {
        val ownerSource = jpeg("journal-owner-source.jpg", android.graphics.Color.RED)
        val victimSource = jpeg("journal-victim-source.jpg", android.graphics.Color.BLUE)
        val ownerSessionId = UUID.randomUUID().toString()
        val victimSessionId = UUID.randomUUID().toString()
        val owner = imports.importUris(
            ownerSessionId,
            "2",
            listOf(Uri.fromFile(ownerSource)),
        ).session!!
        val victim = imports.importUris(
            victimSessionId,
            "2",
            listOf(Uri.fromFile(victimSource)),
        ).session!!
        val victimPhoto = victim.photos.single()
        val victimCopy = sessions.resolvePhotoPath(victimPhoto)
        val victimHash = sha256(victimCopy)

        // The filename and parent claim this belongs to ownerSessionId, but the JSON targets
        // a different session. Recovery must reject this corrupt intent before acting on it.
        val mismatched = JSONObject().apply {
            put("sessionId", victimSessionId)
            put("photoId", victimPhoto.photoId)
            put("path", victimPhoto.path)
        }
        File(
            context.filesDir,
            "import_removals/$ownerSessionId/${owner.photos.single().photoId}.json",
        ).apply {
            parentFile!!.mkdirs()
            writeText(mismatched.toString())
        }

        val recovered = imports.recover(ownerSessionId)
        assertEquals(1, recovered.failures.size)
        assertEquals(PhotoImportFailureReason.STORAGE, recovered.failures.single().reason)
        assertTrue(victimCopy.isFile)
        assertEquals(victimHash, sha256(victimCopy))
        assertEquals(
            listOf(victimPhoto.photoId),
            sessions.getById(victimSessionId)!!.photos.map { it.photoId },
        )
    }

    @Test fun failedRemovalReplayDoesNotResurrectPhotoFromCommittedImportJournal() = runBlocking {
        val source = jpeg("failed-removal-source.jpg", android.graphics.Color.LTGRAY)
        val sourceHash = sha256(source)
        val sessionId = UUID.randomUUID().toString()
        val imported = imports.importUris(
            sessionId,
            "2",
            listOf(Uri.fromFile(source)),
        ).session!!
        val photo = imported.photos.single()
        val ownedCopy = sessions.resolvePhotoPath(photo)
        val importJournal = File(
            context.filesDir,
            "import_journals/$sessionId/${photo.photoId}.json",
        )
        assertEquals(
            "SESSION_COMMITTED",
            JSONObject(importJournal.readText()).getString("status"),
        )

        val withoutReference = sessions.removeImportedPhoto(
            sessionId,
            imported.revision,
            photo.photoId,
        )
        assertTrue(withoutReference.photos.isEmpty())
        assertTrue(ownedCopy.isFile)
        writeRemovalJournal(
            sessionId,
            photo.photoId,
            "imports/$sessionId/not-${photo.photoId}.jpg",
        )

        val recovered = PhotoImportRepository(
            context,
            SessionDocumentRepository(context),
        ).recover(sessionId)
        assertEquals(1, recovered.failures.size)
        assertEquals(PhotoImportFailureReason.STORAGE, recovered.failures.single().reason)
        assertTrue(recovered.recoveredPhotoIds.isEmpty())
        assertTrue(recovered.session!!.photos.isEmpty())
        assertTrue(SessionDocumentRepository(context).getById(sessionId)!!.photos.isEmpty())
        assertTrue(ownedCopy.isFile)
        assertEquals(sourceHash, sha256(source))
        assertTrue(importJournal.isFile)
    }

    @Test fun duplicateAndFallbackOverSelectionNeverSilentlyTruncate() = runBlocking {
        val firstId = UUID.randomUUID().toString()
        val first = jpeg("duplicate.jpg", android.graphics.Color.GREEN)
        val duplicateResult = imports.importUris(firstId, "2", listOf(Uri.fromFile(first), Uri.fromFile(first)))
        assertEquals(1, duplicateResult.session!!.photos.size)
        assertEquals(listOf(Uri.fromFile(first).toString()), duplicateResult.duplicates)

        val secondId = UUID.randomUUID().toString()
        val sources = listOf(
            jpeg("over-1.jpg", android.graphics.Color.RED),
            jpeg("over-2.jpg", android.graphics.Color.GREEN),
            jpeg("over-3.jpg", android.graphics.Color.BLUE),
        )
        val overflow = imports.importUris(secondId, "2", sources.map(Uri::fromFile))
        assertNull(overflow.session)
        assertEquals(3, overflow.failures.size)
        assertTrue(overflow.failures.all { it.reason == PhotoImportFailureReason.TOO_MANY_SELECTIONS })
        assertFalse(File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "Pocket4Cut/imports/$secondId").exists())
    }

    @Test fun unsupportedImageDoesNotCreateSessionOrLeavePendingCopy() = runBlocking {
        val id = UUID.randomUUID().toString()
        val gif = File(testRoot, "not-supported.gif").apply {
            writeBytes("GIF89a".toByteArray() + ByteArray(32))
        }
        val result = imports.importUris(id, "2", listOf(Uri.fromFile(gif)))
        assertNull(result.session)
        assertEquals(PhotoImportFailureReason.ANIMATED_IMAGE, result.failures.single().reason)
        assertNull(sessions.getById(id))
        assertFalse(File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "Pocket4Cut/imports/$id").walkTopDown().any { it.isFile })
    }

    @Test fun animatedWebpAvifAndBmpSignaturesAreExplicitlyRejected() = runBlocking {
        val id = UUID.randomUUID().toString()
        val animatedWebp = File(testRoot, "animated.webp").apply {
            val bytes = ByteArray(32)
            "RIFF".toByteArray().copyInto(bytes, destinationOffset = 0)
            "WEBP".toByteArray().copyInto(bytes, destinationOffset = 8)
            "VP8X".toByteArray().copyInto(bytes, destinationOffset = 12)
            bytes[20] = 0x02
            writeBytes(bytes)
        }
        val avif = File(testRoot, "unsupported.avif").apply {
            val bytes = ByteArray(24)
            "ftyp".toByteArray().copyInto(bytes, destinationOffset = 4)
            "avif".toByteArray().copyInto(bytes, destinationOffset = 8)
            writeBytes(bytes)
        }
        val bmp = File(testRoot, "unsupported.bmp").apply {
            writeBytes("BM".toByteArray() + ByteArray(30))
        }

        val result = imports.importUris(
            id,
            "6",
            listOf(animatedWebp, avif, bmp).map { Uri.fromFile(it) },
        )
        assertNull(result.session)
        assertEquals(3, result.failures.size)
        assertEquals(
            PhotoImportFailureReason.ANIMATED_IMAGE,
            result.failures.single { it.uri == Uri.fromFile(animatedWebp).toString() }.reason,
        )
        assertEquals(
            PhotoImportFailureReason.UNSUPPORTED_FORMAT,
            result.failures.single { it.uri == Uri.fromFile(avif).toString() }.reason,
        )
        assertEquals(
            PhotoImportFailureReason.UNSUPPORTED_FORMAT,
            result.failures.single { it.uri == Uri.fromFile(bmp).toString() }.reason,
        )
        assertNull(sessions.getById(id))
        assertFalse(File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "Pocket4Cut/imports/$id",
        ).walkTopDown().any { it.isFile })
    }

    @Suppress("DEPRECATION")
    @Test fun pngAndStaticWebpAreImportedWithoutReencoding() = runBlocking {
        val id = UUID.randomUUID().toString()
        val png = image("source.png", Bitmap.CompressFormat.PNG)
        val webp = image("source.webp", Bitmap.CompressFormat.WEBP)
        val hashes = listOf(sha256(png), sha256(webp))
        val result = imports.importUris(id, "2", listOf(Uri.fromFile(png), Uri.fromFile(webp)))
        assertTrue(result.failures.isEmpty())
        val files = result.session!!.photos.map(sessions::resolvePhotoPath)
        assertEquals(listOf("png", "webp"), files.map { it.extension })
        assertEquals(hashes, files.map(::sha256))
        assertEquals(hashes, listOf(sha256(png), sha256(webp)))
    }

    @Test fun jpegWithMotionPhotoPayloadAfterEoiIsImportedByteForByte() = runBlocking {
        val id = UUID.randomUUID().toString()
        val source = jpeg("motion-photo.jpg", android.graphics.Color.CYAN)
        val mp4LikePayload = byteArrayOf(
            0x00, 0x00, 0x00, 0x18,
            'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
            'm'.code.toByte(), 'p'.code.toByte(), '4'.code.toByte(), '2'.code.toByte(),
            0x00, 0x00, 0x00, 0x00,
            'm'.code.toByte(), 'p'.code.toByte(), '4'.code.toByte(), '2'.code.toByte(),
            'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
        ) + ByteArray(256) { index -> (index and 0xff).toByte() }
        source.appendBytes(mp4LikePayload)
        val sourceHash = sha256(source)

        val result = imports.importUris(id, "2", listOf(Uri.fromFile(source)))
        assertTrue(result.failures.isEmpty())
        val imported = result.session!!.photos.single()
        val ownedCopy = sessions.resolvePhotoPath(imported)
        assertEquals("jpg", ownedCopy.extension)
        assertEquals(source.length(), ownedCopy.length())
        assertEquals(sourceHash, sha256(ownedCopy))
        assertEquals(sourceHash, sha256(source))
    }

    @Test fun emptyAndCorruptImagesAreRejectedWithoutDraft() = runBlocking {
        val id = UUID.randomUUID().toString()
        val empty = File(testRoot, "empty.jpg").apply { writeBytes(byteArrayOf()) }
        val corrupt = File(testRoot, "corrupt.jpg").apply {
            writeBytes(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x00, 0x01))
        }
        val result = imports.importUris(id, "2", listOf(Uri.fromFile(empty), Uri.fromFile(corrupt)))
        assertNull(result.session)
        assertEquals(2, result.failures.size)
        assertTrue(result.failures.all {
            it.reason == PhotoImportFailureReason.INVALID_IMAGE ||
                it.reason == PhotoImportFailureReason.UNSUPPORTED_FORMAT
        })
        assertNull(sessions.getById(id))
    }

    @Test fun boundsReadableButTruncatedPixelPayloadIsRejected() = runBlocking {
        val id = UUID.randomUUID().toString()
        val complete = jpeg("complete-for-truncation.jpg", android.graphics.Color.DKGRAY)
        val completeBytes = complete.readBytes()
        val startOfScan = (0 until completeBytes.lastIndex).first { index ->
            completeBytes[index].toInt() and 0xff == 0xff &&
                completeBytes[index + 1].toInt() and 0xff == 0xda
        }
        val scanHeaderLength =
            ((completeBytes[startOfScan + 2].toInt() and 0xff) shl 8) or
                (completeBytes[startOfScan + 3].toInt() and 0xff)
        val truncated = File(testRoot, "bounds-only.jpg").apply {
            // Preserve SOF and the complete SOS header, but remove all entropy-coded pixels.
            writeBytes(completeBytes.copyOf(startOfScan + 2 + scanHeaderLength))
        }
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(truncated.absolutePath, bounds)
        assertTrue(bounds.outWidth > 0)
        assertTrue(bounds.outHeight > 0)

        val result = imports.importUris(id, "2", listOf(Uri.fromFile(truncated)))
        assertNull(result.session)
        assertEquals(PhotoImportFailureReason.INVALID_IMAGE, result.failures.single().reason)
        assertNull(sessions.getById(id))
    }

    @Test fun deletingAlbumSessionPreservesExternalSourcesAndOtherSessionCopies() = runBlocking {
        val firstSource = jpeg("session-one.jpg", android.graphics.Color.YELLOW)
        val secondSource = jpeg("session-two.jpg", android.graphics.Color.CYAN)
        val firstId = UUID.randomUUID().toString()
        val secondId = UUID.randomUUID().toString()
        val first = imports.importUris(firstId, "2", listOf(Uri.fromFile(firstSource))).session!!
        val second = imports.importUris(secondId, "2", listOf(Uri.fromFile(secondSource))).session!!
        val firstCopy = sessions.resolvePhotoPath(first.photos.single())
        val secondCopy = sessions.resolvePhotoPath(second.photos.single())
        val secondCopyHash = sha256(secondCopy)

        sessions.requestDelete(firstId, first.revision)

        assertFalse(firstCopy.exists())
        assertTrue(secondCopy.exists())
        assertEquals(secondCopyHash, sha256(secondCopy))
        assertTrue(firstSource.exists())
        assertTrue(secondSource.exists())
        assertEquals(secondId, sessions.getById(secondId)?.sessionId)
    }

    private fun jpeg(name: String, color: Int): File {
        val file = File(testRoot, name)
        val bitmap = Bitmap.createBitmap(8, 6, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
        bitmap.recycle()
        return file
    }

    private fun image(name: String, format: Bitmap.CompressFormat): File {
        val file = File(testRoot, name)
        val bitmap = Bitmap.createBitmap(8, 6, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.MAGENTA)
        file.outputStream().use { assertTrue(bitmap.compress(format, 95, it)) }
        bitmap.recycle()
        return file
    }

    private fun importFile(sessionId: String, fileName: String): File = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "Pocket4Cut/imports/$sessionId/$fileName",
    ).apply { parentFile!!.mkdirs() }

    private fun writeImportJournal(
        sessionId: String,
        photoId: String,
        source: File,
        status: String,
        finalPath: String?,
        byteCount: Long?,
        hash: String?,
    ) {
        val journal = JSONObject().apply {
            put("sessionId", sessionId)
            put("photoId", photoId)
            put("frameTypeId", "2")
            put("sourceUri", Uri.fromFile(source).toString())
            put("status", status)
            put("captureIndex", 0)
            put("pendingPath", "imports/$sessionId/.pending/$photoId.part")
            put("finalPath", finalPath ?: JSONObject.NULL)
            put("byteCount", byteCount ?: JSONObject.NULL)
            put("sha256", hash ?: JSONObject.NULL)
            put("createdAt", 1_700_000_000_000L)
            put("persistableGrant", false)
        }
        File(context.filesDir, "import_journals/$sessionId/$photoId.json").apply {
            parentFile!!.mkdirs()
            writeText(journal.toString())
        }
    }

    private fun writeRemovalJournal(sessionId: String, photoId: String, path: String) {
        val journal = JSONObject().apply {
            put("sessionId", sessionId)
            put("photoId", photoId)
            put("path", path)
        }
        File(context.filesDir, "import_removals/$sessionId/$photoId.json").apply {
            parentFile!!.mkdirs()
            writeText(journal.toString())
        }
    }

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256")
        .digest(file.readBytes()).joinToString("") { "%02x".format(it) }
}
