package com.pocket4cut

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.export.ExportOutcome
import com.pocket4cut.data.export.GalleryExporter
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.ExportOperation
import com.pocket4cut.domain.model.ExportStatus
import com.pocket4cut.domain.model.ResultRecord
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionStage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class GalleryExporterInstrumentedTest {
    @Test fun deletedCompletedMediaRowCanBeRecreatedOnceWithSameOperation() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= 29)
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val testRoot = File(app.cacheDir, "gallery-deleted-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = isolatedContext(app, testRoot)
        val id = UUID.randomUUID().toString()
        val resultId = UUID.randomUUID().toString()
        val resultPath = "results/${id}_${resultId}.jpg"
        val repository = SessionDocumentRepository(context)
        val exporter = GalleryExporter(context, repository)
        try {
            val bytes = jpegBytes()
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Pocket4Cut/$resultPath")
                .apply { parentFile!!.mkdirs(); writeBytes(bytes) }
            repository.create(SessionDocument(
                sessionId = id, createdAt = 1_700_000_000_000L,
                captureCount = 4, selectedCount = 2, stage = SessionStage.RESULT,
                results = listOf(ResultRecord(resultId, 0, resultPath, 4, 4, 1L)),
            ))
            val first = exporter.export(id, resultId) as ExportOutcome.Saved
            val operation = repository.getById(id)!!.exportOperations.single()
            assertTrue(exporter.isNormalCopyVerified(id, resultId))
            assertEquals(1, context.contentResolver.delete(first.uri, null, null))
            assertFalse(exporter.isNormalCopyVerified(id, resultId))
            val retried = exporter.export(id, resultId) as ExportOutcome.Saved
            assertTrue(first.uri != retried.uri)
            assertEquals(1, countRows(context, operation.displayName))
            assertEquals(operation.operationId, repository.getById(id)!!.exportOperations.single().operationId)
            assertArrayEquals(bytes, context.contentResolver.openInputStream(retried.uri)!!.use { it.readBytes() })
            assertTrue(exporter.export(id, resultId) is ExportOutcome.AlreadySaved)
        } finally {
            repository.getById(id)?.exportOperations?.forEach { deleteRows(context, it.displayName) }
            assertTrue(testRoot.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            testRoot.deleteRecursively()
        }
    }

    @Test fun changedCompletedMediaRowIsNotDuplicatedOrOverwritten() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= 29)
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val testRoot = File(app.cacheDir, "gallery-changed-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = isolatedContext(app, testRoot)
        val id = UUID.randomUUID().toString()
        val resultId = UUID.randomUUID().toString()
        val resultPath = "results/${id}_${resultId}.jpg"
        val repository = SessionDocumentRepository(context)
        val exporter = GalleryExporter(context, repository)
        try {
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Pocket4Cut/$resultPath")
                .apply { parentFile!!.mkdirs(); writeBytes(jpegBytes()) }
            repository.create(SessionDocument(
                sessionId = id, createdAt = 1_700_000_000_000L,
                captureCount = 4, selectedCount = 2, stage = SessionStage.RESULT,
                results = listOf(ResultRecord(resultId, 0, resultPath, 4, 4, 1L)),
            ))
            val first = exporter.export(id, resultId) as ExportOutcome.Saved
            val changed = "externally changed copy".toByteArray()
            context.contentResolver.openOutputStream(first.uri, "wt")!!.use { it.write(changed) }
            assertFalse(exporter.isNormalCopyVerified(id, resultId))
            assertTrue(exporter.export(id, resultId) is ExportOutcome.NeedsRecovery)
            assertEquals(1, countRows(context, repository.getById(id)!!.exportOperations.single().displayName))
            assertArrayEquals(changed, context.contentResolver.openInputStream(first.uri)!!.use { it.readBytes() })
        } finally {
            repository.getById(id)?.exportOperations?.forEach { deleteRows(context, it.displayName) }
            assertTrue(testRoot.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            testRoot.deleteRecursively()
        }
    }

    @Test fun preparedExportWithoutMediaRowResumesAfterRestartOnlyOnce() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= 29)
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val testRoot = File(app.cacheDir, "gallery-prepared-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = isolatedContext(app, testRoot)
        val id = UUID.randomUUID().toString()
        val resultId = UUID.randomUUID().toString()
        val operationId = UUID.randomUUID().toString()
        val displayName = "Pocket4Cut_${resultId}_${operationId}.jpg"
        var savedUri: Uri? = null
        try {
            val bytes = jpegBytes()
            val resultPath = "results/${id}_${resultId}.jpg"
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Pocket4Cut/$resultPath")
                .apply { parentFile!!.mkdirs(); writeBytes(bytes) }
            val repository = SessionDocumentRepository(context)
            repository.create(SessionDocument(
                sessionId = id, createdAt = 1_700_000_000_000L,
                captureCount = 4, selectedCount = 2, stage = SessionStage.RESULT,
                results = listOf(ResultRecord(resultId, 0, resultPath, 4, 4, 1L)),
                exportOperations = listOf(ExportOperation(
                    operationId, resultId, ExportStatus.PREPARED, null, displayName, 1L,
                )),
            ))

            val first = GalleryExporter(context, repository).export(id, resultId)
            assertTrue(first is ExportOutcome.Saved)
            savedUri = (first as ExportOutcome.Saved).uri
            assertArrayEquals(bytes, context.contentResolver.openInputStream(savedUri!!)!!.use { it.readBytes() })
            val second = GalleryExporter(context, SessionDocumentRepository(context)).export(id, resultId)
            assertTrue(second is ExportOutcome.AlreadySaved)
            assertEquals(savedUri, (second as ExportOutcome.AlreadySaved).uri)
            assertEquals(1, countRows(context, displayName))
        } finally {
            deleteRows(context, displayName)
            assertTrue(testRoot.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            testRoot.deleteRecursively()
        }
    }

    @Test fun preparedExportReusesItsPendingMediaRowAfterRestart() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= 29)
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val testRoot = File(app.cacheDir, "gallery-pending-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = isolatedContext(app, testRoot)
        val id = UUID.randomUUID().toString()
        val resultId = UUID.randomUUID().toString()
        val operationId = UUID.randomUUID().toString()
        val displayName = "Pocket4Cut_${resultId}_${operationId}.jpg"
        try {
            val bytes = jpegBytes()
            val resultPath = "results/${id}_${resultId}.jpg"
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Pocket4Cut/$resultPath")
                .apply { parentFile!!.mkdirs(); writeBytes(bytes) }
            val repository = SessionDocumentRepository(context)
            repository.create(SessionDocument(
                sessionId = id, createdAt = 1_700_000_000_000L,
                captureCount = 4, selectedCount = 2, stage = SessionStage.RESULT,
                results = listOf(ResultRecord(resultId, 0, resultPath, 4, 4, 1L)),
                exportOperations = listOf(ExportOperation(
                    operationId, resultId, ExportStatus.PREPARED, null, displayName, 1L,
                )),
            ))
            val pending = context.contentResolver.insert(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pocket4Cut/")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                },
            ) ?: error("Could not create isolated pending row")
            context.contentResolver.openOutputStream(pending)?.use { it.write(bytes) }
                ?: error("Could not write isolated pending row")

            val outcome = GalleryExporter(context, repository).export(id, resultId)
            assertTrue(outcome is ExportOutcome.Saved)
            assertEquals(pending, (outcome as ExportOutcome.Saved).uri)
            assertEquals(1, countRows(context, displayName))
        } finally {
            deleteRows(context, displayName)
            assertTrue(testRoot.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            testRoot.deleteRecursively()
        }
    }

    @Test fun recoveryWithoutRecordedUriCompletesItsPartialPendingRow() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= 29)
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val testRoot = File(app.cacheDir, "gallery-recovery-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = isolatedContext(app, testRoot)
        val id = UUID.randomUUID().toString()
        val resultId = UUID.randomUUID().toString()
        val operationId = UUID.randomUUID().toString()
        val displayName = "Pocket4Cut_${resultId}_${operationId}.jpg"
        try {
            val bytes = jpegBytes()
            val resultPath = "results/${id}_${resultId}.jpg"
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Pocket4Cut/$resultPath")
                .apply { parentFile!!.mkdirs(); writeBytes(bytes) }
            val repository = SessionDocumentRepository(context)
            repository.create(SessionDocument(
                sessionId = id, createdAt = 1_700_000_000_000L,
                captureCount = 4, selectedCount = 2, stage = SessionStage.RESULT,
                results = listOf(ResultRecord(resultId, 0, resultPath, 4, 4, 1L)),
                exportOperations = listOf(ExportOperation(
                    operationId, resultId, ExportStatus.NEEDS_RECOVERY, null, displayName, 1L,
                )),
            ))
            val pending = context.contentResolver.insert(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pocket4Cut/")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                },
            ) ?: error("Could not create isolated pending row")
            context.contentResolver.openOutputStream(pending)?.use { it.write(bytes, 0, bytes.size / 2) }
                ?: error("Could not write partial pending row")

            val outcome = GalleryExporter(context, repository).export(id, resultId)
            assertTrue(outcome is ExportOutcome.Saved)
            assertEquals(pending, (outcome as ExportOutcome.Saved).uri)
            assertEquals(1, countRows(context, displayName))
            assertArrayEquals(bytes, context.contentResolver.openInputStream(pending)!!.use { it.readBytes() })
            assertEquals(ExportStatus.COMPLETED, repository.getById(id)!!.exportOperations.single().status)
        } finally {
            deleteRows(context, displayName)
            assertTrue(testRoot.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            testRoot.deleteRecursively()
        }
    }

    @Test fun completedOperationRejectsUnrelatedOrPendingMediaRow() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= 29)
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val testRoot = File(app.cacheDir, "gallery-export-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(testRoot, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(testRoot, "external/$type").apply { mkdirs() }
        }
        var unrelated: Uri? = null
        try {
            val id = UUID.randomUUID().toString()
            val resultId = UUID.randomUUID().toString()
            val operationId = UUID.randomUUID().toString()
            val bytes = ByteArrayOutputStream().use { stream ->
                val image = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                try {
                    image.eraseColor(android.graphics.Color.BLUE)
                    assertTrue(image.compress(Bitmap.CompressFormat.JPEG, 95, stream))
                    stream.toByteArray()
                } finally { image.recycle() }
            }
            val resultFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "Pocket4Cut/results/$resultId.jpg")
            resultFile.parentFile!!.mkdirs()
            resultFile.writeBytes(bytes)
            val resolver = context.contentResolver
            unrelated = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "unrelated_${UUID.randomUUID()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pocket4Cut/")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }) ?: error("Could not create isolated QA media row")
            resolver.openOutputStream(unrelated!!)?.use { it.write(bytes) }
                ?: error("Could not write isolated QA media row")
            val repository = SessionDocumentRepository(context)
            repository.create(SessionDocument(
                sessionId = id, createdAt = 1_700_000_000_000L,
                captureCount = 4, selectedCount = 2, stage = SessionStage.RESULT,
                results = listOf(ResultRecord(resultId, 0, "results/$resultId.jpg", 4, 4, 1L)),
                exportOperations = listOf(ExportOperation(
                    operationId, resultId, ExportStatus.COMPLETED, unrelated.toString(),
                    "Pocket4Cut_${resultId}_${operationId}.jpg", 1L,
                )),
            ))
            val outcome = GalleryExporter(context, repository).export(id, resultId)
            assertTrue(outcome is ExportOutcome.NeedsRecovery)
            val after = resolver.openInputStream(unrelated!!)?.use { it.readBytes() }
            assertArrayEquals(bytes, after)
            val expectedName = "Pocket4Cut_${resultId}_${operationId}.jpg"
            assertTrue(resolver.update(unrelated!!, ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, expectedName)
            }, null, null) == 1)
            assertTrue(GalleryExporter(context, repository).export(id, resultId) is ExportOutcome.NeedsRecovery)
        } finally {
            unrelated?.let { context.contentResolver.delete(it, null, null) }
            assertTrue(testRoot.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            testRoot.deleteRecursively()
        }
    }

    private fun isolatedContext(app: Context, root: File): Context = object : ContextWrapper(app) {
        override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
        override fun getExternalFilesDir(type: String?): File = File(root, "external/$type").apply { mkdirs() }
    }

    private fun jpegBytes(): ByteArray = ByteArrayOutputStream().use { stream ->
        val image = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        try {
            image.eraseColor(android.graphics.Color.BLUE)
            assertTrue(image.compress(Bitmap.CompressFormat.JPEG, 95, stream))
            stream.toByteArray()
        } finally { image.recycle() }
    }

    @Suppress("DEPRECATION") // Includes in-progress rows on API 29 as well as later releases.
    private fun matchingRows(context: Context, displayName: String): List<Uri> {
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val rows = mutableListOf<Uri>()
        context.contentResolver.query(MediaStore.setIncludePending(collection),
            arrayOf(MediaStore.Images.Media._ID), "${MediaStore.Images.Media.DISPLAY_NAME}=?",
            arrayOf(displayName), null)?.use { cursor ->
            val column = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                rows += android.content.ContentUris.withAppendedId(collection, cursor.getLong(column))
            }
        }
        return rows
    }

    private fun countRows(context: Context, displayName: String): Int = matchingRows(context, displayName).size

    private fun deleteRows(context: Context, displayName: String) {
        matchingRows(context, displayName).forEach { context.contentResolver.delete(it, null, null) }
    }
}
