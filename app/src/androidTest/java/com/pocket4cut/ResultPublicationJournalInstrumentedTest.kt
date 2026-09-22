package com.pocket4cut

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.data.local.SessionCorruptException
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.domain.model.ResultRecord
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionStage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ResultPublicationJournalInstrumentedTest {
    @Test fun damagedNewJpegRequiresRecoveryWithoutHidingExistingResult() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val testRoot = File(app.cacheDir, "damaged-result-journal-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(testRoot, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(testRoot, "external/$type").apply { mkdirs() }
        }
        try {
            val id = UUID.randomUUID().toString()
            val previousId = UUID.randomUUID().toString()
            val nextId = UUID.randomUUID().toString()
            val pictures = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "Pocket4Cut/results").apply { mkdirs() }
            val previousFile = File(pictures, "${id}_${previousId}.jpg")
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(android.graphics.Color.RED)
                previousFile.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
            } finally { bitmap.recycle() }
            val previousBytes = previousFile.readBytes()
            val previous = ResultRecord(previousId, 0, "results/${previousFile.name}", 8, 8, 1L)
            val repository = SessionDocumentRepository(context)
            val created = repository.create(SessionDocument(
                sessionId = id, createdAt = 1_700_000_000_000L,
                captureCount = 4, selectedCount = 2, stage = SessionStage.RESULT,
                results = listOf(previous),
            ))
            val next = ResultRecord(nextId, created.revision,
                "results/${id}_${nextId}.jpg", 8, 8, System.currentTimeMillis())
            repository.prepareResultPublication(id, created.revision, next)
            val damagedFile = File(pictures, "${id}_${nextId}.jpg")
            damagedFile.writeBytes(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xd9.toByte()))
            val journal = File(context.filesDir, "result_publications/$id/$nextId.json")

            val restarted = SessionDocumentRepository(context)
            val visible = restarted.getById(id)!!
            assertEquals(SessionStage.NEEDS_RECOVERY, visible.stage)
            assertEquals(listOf(previous), visible.results)
            assertEquals(SessionStage.NEEDS_RECOVERY, restarted.list().single().stage)
            assertTrue(journal.isFile)
            assertArrayEquals(previousBytes, previousFile.readBytes())
            var blocked = false
            try { restarted.update(id, created.revision) { it.copy(draft = it.draft.copy(caption = "changed")) } }
            catch (_: SessionCorruptException) { blocked = true }
            assertTrue(blocked)
        } finally {
            assertTrue(testRoot.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            testRoot.deleteRecursively()
        }
    }

    @Test fun publishedFileReplaysOnceAfterRestartAndPreservesPreviousResult() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val testRoot = File(app.cacheDir, "result-journal-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(testRoot, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(testRoot, "external/$type").apply { mkdirs() }
        }
        try {
            val id = UUID.randomUUID().toString()
            val previousId = UUID.randomUUID().toString()
            val nextId = UUID.randomUUID().toString()
            val pictures = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "Pocket4Cut/results").apply { mkdirs() }
            val previousFile = File(pictures, "${id}_${previousId}.jpg")
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(android.graphics.Color.RED)
                previousFile.outputStream().use { output ->
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
                }
                val previousBytes = previousFile.readBytes()
                val repository = SessionDocumentRepository(context)
                val previous = ResultRecord(previousId, 0, "results/${previousFile.name}", 8, 8, 1L)
                val created = repository.create(SessionDocument(
                    sessionId = id, createdAt = 1_700_000_000_000L,
                    captureCount = 4, selectedCount = 2, stage = SessionStage.RESULT,
                    results = listOf(previous),
                ))
                val next = ResultRecord(nextId, created.revision,
                    "results/${id}_${nextId}.jpg", 8, 8, System.currentTimeMillis())
                repository.prepareResultPublication(id, created.revision, next)
                assertEquals(listOf(previous), repository.getById(id)?.results)
                val journal = File(context.filesDir, "result_publications/$id/$nextId.json")
                assertTrue(journal.isFile)

                bitmap.eraseColor(android.graphics.Color.BLUE)
                val publishedPath = FileImageStorage(context).saveImmutableResult(bitmap, id, nextId)
                val nextBytes = File(publishedPath).readBytes()

                // A new repository instance simulates the next process reading durable files.
                val restarted = SessionDocumentRepository(context)
                val recovered = restarted.getById(id)!!
                assertEquals(listOf(previous, next), recovered.results)
                assertEquals(created.revision + 1, recovered.revision)
                assertFalse(journal.exists())
                assertArrayEquals(previousBytes, previousFile.readBytes())
                assertArrayEquals(nextBytes, File(publishedPath).readBytes())
                assertEquals(recovered.revision, restarted.getById(id)?.revision)
                assertEquals(recovered.revision, restarted.completeResultPublication(id, nextId).revision)
                assertEquals(2, restarted.list().single().results.size)
            } finally { bitmap.recycle() }
        } finally {
            assertTrue(testRoot.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            testRoot.deleteRecursively()
        }
    }
}
