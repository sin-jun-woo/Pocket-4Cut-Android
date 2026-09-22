package com.pocket4cut

import android.content.Context
import android.content.ContextWrapper
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.ResultRecord
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.presentation.result.findResultLink
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultLinkInstrumentedTest {
    @Test fun unrelatedCorruptSessionDoesNotBlockHealthyResultLink() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(app.cacheDir, "result-link-test-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(root, "external/$type").apply { mkdirs() }
        }
        try {
            val sessionId = UUID.randomUUID().toString()
            val resultId = UUID.randomUUID().toString()
            val path = "results/$resultId.jpg"
            val resultFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "Pocket4Cut/$path").apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1, 2, 3)) }
            val repository = SessionDocumentRepository(context)
            repository.create(SessionDocument(
                sessionId = sessionId, createdAt = 1_700_000_000_000L,
                captureCount = 4, selectedCount = 2, stage = SessionStage.RESULT,
                results = listOf(ResultRecord(resultId, 0, path, 4, 4, 1L)),
            ))
            val corruptId = UUID.randomUUID().toString()
            File(context.filesDir, "session_documents/$corruptId.json").writeText("{broken")

            assertEquals(sessionId to resultId, findResultLink(SessionDocumentRepository(context), resultFile))
            assertTrue(File(context.filesDir, "session_documents/$corruptId.json").exists())
        } finally {
            assertTrue(root.canonicalPath.startsWith(app.cacheDir.canonicalPath + File.separator))
            root.deleteRecursively()
        }
    }
}
