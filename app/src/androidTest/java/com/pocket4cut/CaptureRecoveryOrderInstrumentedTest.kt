package com.pocket4cut

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.presentation.capture.CaptureFileRecovery
import com.pocket4cut.presentation.capture.CaptureSequenceMismatch
import com.pocket4cut.presentation.capture.RecordedCaptureUnavailable
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CaptureRecoveryOrderInstrumentedTest {
    private lateinit var root: File
    private lateinit var context: Context
    private lateinit var storage: FileImageStorage
    private lateinit var sessions: SessionDocumentRepository

    @Before fun setUp() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(app.cacheDir, "capture-order-test-${UUID.randomUUID()}").apply { mkdirs() }
        context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(root, "external/$type").apply { mkdirs() }
        }
        storage = FileImageStorage(context)
        sessions = SessionDocumentRepository(context)
    }

    @After fun tearDown() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(root.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
        root.deleteRecursively()
    }

    @Test fun missingRecordedSourceStopsRecoveryWithoutReplacingOtherPhotos() = runBlocking {
        val id = UUID.randomUUID().toString()
        var document = sessions.create(SessionDocument(
            sessionId = id, createdAt = 1_700_000_000_000L,
            captureCount = 4, selectedCount = 2,
        ))
        val bytes = jpegBytes()
        val first = published(id, 1, bytes)
        val second = published(id, 2, bytes)
        document = sessions.appendCapture(id, document.revision,
            PhotoRef(UUID.randomUUID().toString(), "captures/$id/${first.name}", 0))
        document = sessions.appendCapture(id, document.revision,
            PhotoRef(UUID.randomUUID().toString(), "captures/$id/${second.name}", 1))
        val secondBefore = second.readBytes()
        assertTrue(first.delete()) // Fault injection is confined to this isolated test directory.

        var stopped = false
        try { CaptureFileRecovery(storage, sessions).recover(document) }
        catch (t: RecordedCaptureUnavailable) {
            assertEquals(1, t.index)
            stopped = true
        }
        assertTrue(stopped)
        assertEquals(2, sessions.getById(id)!!.photos.size)
        assertArrayEquals(secondBefore, second.readBytes())

        first.writeBytes(bytes)
        second.writeBytes(byteArrayOf(0x12, 0x34, 0x56, 0x78))
        var damagedRecordedStopped = false
        try { CaptureFileRecovery(storage, sessions).recover(document) }
        catch (t: RecordedCaptureUnavailable) {
            assertEquals(2, t.index)
            damagedRecordedStopped = true
        }
        assertTrue(damagedRecordedStopped)
        assertArrayEquals(bytes, first.readBytes())
        assertEquals(2, sessions.getById(id)!!.photos.size)
    }

    @Test fun outOfOrderUnrecordedFileIsPreservedAndNeverAppended() = runBlocking {
        val id = UUID.randomUUID().toString()
        var document = sessions.create(SessionDocument(
            sessionId = id, createdAt = 1_700_000_000_000L,
            captureCount = 4, selectedCount = 2,
        ))
        val bytes = jpegBytes()
        val first = published(id, 1, bytes)
        document = sessions.appendCapture(id, document.revision,
            PhotoRef(UUID.randomUUID().toString(), "captures/$id/${first.name}", 0))
        val third = published(id, 3, bytes)
        val thirdBefore = third.readBytes()

        var stopped = false
        try { CaptureFileRecovery(storage, sessions).recover(document) }
        catch (_: CaptureSequenceMismatch) { stopped = true }
        assertTrue(stopped)
        assertEquals(listOf(0), sessions.getById(id)!!.photos.map { it.captureIndex })
        assertArrayEquals(thirdBefore, third.readBytes())
        assertFalse(storage.createCaptureFile(id, 2).exists())
    }

    @Test fun contiguousPublishedFileAfterCrashIsAppendedOnlyOnce() = runBlocking {
        val id = UUID.randomUUID().toString()
        var document = sessions.create(SessionDocument(
            sessionId = id, createdAt = 1_700_000_000_000L,
            captureCount = 4, selectedCount = 2,
        ))
        val bytes = jpegBytes()
        val first = published(id, 1, bytes)
        document = sessions.appendCapture(id, document.revision,
            PhotoRef(UUID.randomUUID().toString(), "captures/$id/${first.name}", 0))
        val second = published(id, 2, bytes)
        val secondBefore = second.readBytes()
        val recovery = CaptureFileRecovery(storage, sessions)

        val restored = recovery.recover(document)
        assertEquals(listOf(0, 1), restored.photos.map { it.captureIndex })
        assertEquals(2, recovery.recover(restored).photos.size)
        assertArrayEquals(secondBefore, second.readBytes())
    }

    private suspend fun published(id: String, index: Int, bytes: ByteArray): File {
        val pending = storage.createPendingCaptureFile(id).apply { writeBytes(bytes) }
        return storage.publishCaptureFile(pending, id, index)
    }

    private fun jpegBytes(): ByteArray = ByteArrayOutputStream().use { output ->
        val image = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        try {
            image.eraseColor(android.graphics.Color.RED)
            assertTrue(image.compress(Bitmap.CompressFormat.JPEG, 95, output))
            output.toByteArray()
        } finally { image.recycle() }
    }
}
