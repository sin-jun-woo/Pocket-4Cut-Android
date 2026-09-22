package com.pocket4cut

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.presentation.edit.EditViewModel
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class DraftExitPersistenceInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as Application

    @Test fun immediateBackFlushesDebouncedCaptionAndNavigatesOnce() {
        val id = "exit-${UUID.randomUUID()}"
        val photoIds = List(2) { UUID.randomUUID().toString() }
        val sessions = SessionDocumentRepository(app)
        val storage = FileImageStorage(app)
        val viewModels = ViewModelStore()
        try {
            runBlocking {
                repeat(2) { index ->
                    val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(if (index == 0) Color.RED else Color.BLUE)
                    }
                    try {
                        val jpeg = ByteArrayOutputStream().use { output ->
                            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
                            output.toByteArray()
                        }
                        storage.saveCapture(jpeg, id, index + 1)
                    } finally {
                        bitmap.recycle()
                    }
                }
                sessions.create(SessionDocument(
                    sessionId = id,
                    createdAt = System.currentTimeMillis(),
                    captureCount = 4,
                    selectedCount = 2,
                    photos = photoIds.mapIndexed { index, photoId ->
                        PhotoRef(photoId, "captures/$id/cap_0${index + 1}.jpg", index)
                    },
                    draft = SessionDraft(selectedPhotoIdsInOrder = photoIds),
                ))
            }
            lateinit var viewModel: EditViewModel
            instrumentation.runOnMainSync {
                viewModel = ViewModelProvider(
                    viewModels,
                    ViewModelProvider.AndroidViewModelFactory(app),
                )[EditViewModel::class.java]
                viewModel.init(FrameType.TWO_CUT, id, listOf(0, 1), FrameLayoutId.TWO_HORIZONTAL)
            }
            awaitCondition { viewModel.uiState.value.orderedImages.size == 2 }
            val navigationCount = AtomicInteger()
            instrumentation.runOnMainSync {
                viewModel.setText("즉시 나가기 테스트")
                viewModel.leave { navigationCount.incrementAndGet() }
                viewModel.leave { navigationCount.incrementAndGet() }
            }
            awaitCondition { navigationCount.get() == 1 }
            assertEquals("즉시 나가기 테스트", runBlocking { sessions.getById(id) }?.draft?.caption)
            assertEquals(1, navigationCount.get())
        } finally {
            instrumentation.runOnMainSync { viewModels.clear() }
            runBlocking { sessions.requestDelete(id) }
        }
    }

    private fun awaitCondition(predicate: () -> Boolean) {
        val deadline = android.os.SystemClock.uptimeMillis() + 5_000
        while (!predicate() && android.os.SystemClock.uptimeMillis() < deadline) {
            android.os.SystemClock.sleep(10)
        }
        assertTrue("Timed out waiting for persisted edit", predicate())
    }
}
