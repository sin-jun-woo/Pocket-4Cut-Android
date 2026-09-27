package com.pocket4cut

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.presentation.selection.SelectionScreen
import com.pocket4cut.presentation.selection.SelectionViewModel
import com.pocket4cut.ui.theme.Pocket4CutTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class SelectionRecoveryInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    private lateinit var testRoot: File
    private lateinit var context: Context
    private lateinit var sessions: SessionDocumentRepository
    private lateinit var document: SessionDocument
    private lateinit var viewModel: SelectionViewModel
    private val homeCalls = AtomicInteger()
    private val completedCalls = AtomicInteger()
    private var completedOrder: List<Int>? = null
    private var selectionWriteHook: suspend () -> Unit = {}

    @Before fun setUp() = runBlocking {
        val app = instrumentation.targetContext
        testRoot = File(app.cacheDir, "selection-recovery-${UUID.randomUUID()}").apply { mkdirs() }
        context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(testRoot, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(testRoot, "external/$type").apply { mkdirs() }
        }
        val fixtureApplication = object : Application() {
            override fun getApplicationContext(): Context = context
        }
        sessions = SessionDocumentRepository(context)
        val id = UUID.randomUUID().toString()
        val photos = List(4) { index ->
            PhotoRef(UUID.randomUUID().toString(), "captures/$id/cap_$index.jpg", index).also {
                writePhoto(sessions.resolvePhotoPath(it))
            }
        }
        document = sessions.create(SessionDocument(
            sessionId = id,
            createdAt = 1_700_000_000_000L,
            captureCount = 4,
            selectedCount = 2,
            stage = SessionStage.SELECT,
            photos = photos,
            draft = SessionDraft(selectedPhotoIdsInOrder = listOf(photos[2].photoId, photos[0].photoId)),
        ))
        instrumentation.runOnMainSync {
            viewModel = ViewModelProvider(
                store,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        SelectionViewModel(fixtureApplication) { selectionWriteHook() } as T
                },
            )[SelectionViewModel::class.java]
        }
    }

    @After fun tearDown() {
        instrumentation.runOnMainSync { store.clear() }
        val cache = instrumentation.targetContext.cacheDir.canonicalFile
        assertTrue(testRoot.canonicalPath.startsWith(cache.path + File.separator))
        testRoot.deleteRecursively()
    }

    @Test fun missingPhotoLoadLeavesWithoutErasingStoredSelection() = runBlocking {
        assertTrue(sessions.resolvePhotoPath(document.photos[0]).delete())
        showScreen()
        awaitLoadError()

        compose.onNodeWithText("다시 불러오기").assertIsDisplayed()
        compose.onNodeWithText("홈으로").performClick()
        assertEquals(1, homeCalls.get())
        val preserved = sessions.getById(document.sessionId)!!
        assertEquals(document.revision, preserved.revision)
        assertEquals(document.draft.selectedPhotoIdsInOrder, preserved.draft.selectedPhotoIdsInOrder)
    }

    @Test fun loadRetryRestoresStoredOrderAfterSourceBecomesAvailable() = runBlocking {
        val photo = sessions.resolvePhotoPath(document.photos[0])
        assertTrue(photo.delete())
        showScreen()
        awaitLoadError()

        writePhoto(photo)
        compose.onNodeWithText("다시 불러오기").performClick()
        awaitLoaded()
        assertEquals(listOf(2, 0), viewModel.uiState.value.selectedIndexes)
        compose.onNodeWithContentDescription("촬영 사진 1").assertIsDisplayed()
        compose.onNodeWithText("다음").assertIsEnabled()
        assertEquals(document.revision, sessions.getById(document.sessionId)!!.revision)
    }

    @Test fun saveFailureKeepsPhotosAndDirtyOrderUntilRetrySucceeds() = runBlocking {
        showScreen()
        awaitLoaded()
        val parked = blockDocumentDirectory()

        compose.onNodeWithContentDescription("촬영 사진 1").performClick()
        awaitSaveError()
        compose.onNodeWithContentDescription("촬영 사진 2").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { viewModel.uiState.value.selectedIndexes == listOf(2, 1) }
        assertTrue(viewModel.uiState.value.hasLoaded)
        compose.onNodeWithText("선택 저장 다시 시도").assertIsDisplayed()
        val failedLeave = AtomicInteger()
        compose.runOnIdle {
            viewModel.leave(onSaved = { homeCalls.incrementAndGet() }, onFailed = { failedLeave.incrementAndGet() })
        }
        compose.waitUntil(5_000) { failedLeave.get() == 1 }
        assertEquals(0, homeCalls.get())
        assertEquals(listOf(2, 1), viewModel.uiState.value.selectedIndexes)

        restoreDocumentDirectory(parked)
        compose.onNodeWithText("선택 저장 다시 시도").performClick()
        compose.waitUntil(5_000) { viewModel.uiState.value.errorMessage == null }
        assertEquals(listOf(2, 1), viewModel.uiState.value.selectedIndexes)
        val expectedIds = listOf(document.photos[2].photoId, document.photos[1].photoId)
        assertEquals(expectedIds, sessions.getById(document.sessionId)!!.draft.selectedPhotoIdsInOrder)

        compose.onNodeWithText("다음").performClick()
        compose.waitUntil(5_000) { completedOrder == listOf(2, 1) }
        assertEquals(SessionStage.FRAME, sessions.getById(document.sessionId)!!.stage)
    }

    @Test fun repeatedLoadDoesNotDiscardUnsavedSelectionAndLaterSaveClearsError() = runBlocking {
        showScreen()
        awaitLoaded()
        val parked = blockDocumentDirectory()
        compose.onNodeWithContentDescription("촬영 사진 1").performClick()
        awaitSaveError()

        compose.runOnIdle { viewModel.load(document.sessionId, 2) }
        assertEquals(listOf(2), viewModel.uiState.value.selectedIndexes)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.hasLoaded)

        restoreDocumentDirectory(parked)
        compose.onNodeWithContentDescription("촬영 사진 2").performClick()
        compose.waitUntil(5_000) { viewModel.uiState.value.errorMessage == null }
        assertEquals(listOf(2, 1), viewModel.uiState.value.selectedIndexes)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(
            listOf(document.photos[2].photoId, document.photos[1].photoId),
            sessions.getById(document.sessionId)!!.draft.selectedPhotoIdsInOrder,
        )
    }

    @Test fun completingSelectionBlocksLateToggleDuplicateCompletionAndReload(): Unit = runBlocking {
        showScreen()
        awaitLoaded()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        compose.runOnIdle {
            selectionWriteHook = {
                entered.complete(Unit)
                release.await()
            }
        }

        compose.onNodeWithText("다음").performClick()
        compose.waitUntil(5_000) { entered.isCompleted }
        compose.onNodeWithText("선택 저장 중...").assertIsNotEnabled()
        compose.onNodeWithContentDescription("촬영 사진 1").assertIsNotEnabled()
        compose.onNodeWithContentDescription("작업 잠시 멈추기").assertIsNotEnabled()
        compose.runOnIdle {
            viewModel.toggle(0, 2)
            viewModel.retrySave()
            viewModel.complete { completedCalls.incrementAndGet() }
            viewModel.load(document.sessionId, 2)
            viewModel.leave(onSaved = { homeCalls.incrementAndGet() }, onFailed = {})
            assertEquals(listOf(2, 0), viewModel.uiState.value.selectedIndexes)
            assertTrue(viewModel.uiState.value.isCompleting)
            assertFalse(viewModel.uiState.value.isLoading)
        }
        assertEquals(document.revision, sessions.getById(document.sessionId)!!.revision)

        release.complete(Unit)
        compose.waitUntil(5_000) { completedCalls.get() == 1 }
        compose.runOnIdle {
            // Keep the successful outgoing screen locked until it is actually re-entered.
            viewModel.complete { completedCalls.incrementAndGet() }
            viewModel.toggle(0, 2)
            assertTrue(viewModel.uiState.value.isCompleting)
            assertEquals(listOf(2, 0), viewModel.uiState.value.selectedIndexes)
        }
        val saved = sessions.getById(document.sessionId)!!
        assertEquals(document.revision + 1, saved.revision)
        assertEquals(SessionStage.FRAME, saved.stage)
        assertEquals(document.draft.selectedPhotoIdsInOrder, saved.draft.selectedPhotoIdsInOrder)
        assertEquals(listOf(2, 0), completedOrder)
        assertEquals(1, completedCalls.get())
        assertEquals(0, homeCalls.get())

        compose.runOnIdle { viewModel.load(document.sessionId, 2) }
        awaitLoaded()
        assertFalse(viewModel.uiState.value.isCompleting)
        compose.onNodeWithText("다음").assertIsEnabled()
        compose.onNodeWithContentDescription("촬영 사진 1").assertIsEnabled()
    }

    @Test fun completionWriteFailureUnlocksSelectionAndAllowsEditedRetry(): Unit = runBlocking {
        showScreen()
        awaitLoaded()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        compose.runOnIdle {
            selectionWriteHook = {
                entered.complete(Unit)
                release.await()
                throw IOException("Isolated selection write failure")
            }
        }

        compose.onNodeWithText("다음").performClick()
        compose.waitUntil(5_000) { entered.isCompleted }
        assertTrue(viewModel.uiState.value.isCompleting)
        release.complete(Unit)
        compose.waitUntil(5_000) {
            !viewModel.uiState.value.isCompleting && viewModel.uiState.value.errorMessage != null
        }
        assertEquals(0, completedCalls.get())
        assertEquals(document.revision, sessions.getById(document.sessionId)!!.revision)
        compose.onNodeWithText("다음").assertIsEnabled()
        compose.onNodeWithContentDescription("촬영 사진 1").assertIsEnabled()
        compose.runOnIdle { selectionWriteHook = {} }

        compose.onNodeWithContentDescription("촬영 사진 1").performClick()
        compose.onNodeWithContentDescription("촬영 사진 2").performClick()
        compose.waitUntil(5_000) {
            viewModel.uiState.value.selectedIndexes == listOf(2, 1) &&
                viewModel.uiState.value.errorMessage == null
        }
        compose.onNodeWithText("다음").performClick()
        compose.waitUntil(5_000) { completedCalls.get() == 1 }
        val saved = sessions.getById(document.sessionId)!!
        assertEquals(SessionStage.FRAME, saved.stage)
        assertEquals(
            listOf(document.photos[2].photoId, document.photos[1].photoId),
            saved.draft.selectedPhotoIdsInOrder,
        )
        assertEquals(listOf(2, 1), completedOrder)
        assertEquals(0, homeCalls.get())
    }

    private fun showScreen() {
        compose.setContent {
            Pocket4CutTheme {
                SelectionScreen(
                    frameType = FrameType.TWO_CUT,
                    sessionId = document.sessionId,
                    onBack = { homeCalls.incrementAndGet() },
                    onDone = { completedOrder = it; completedCalls.incrementAndGet() },
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun awaitLoaded() {
        compose.waitUntil(5_000) { viewModel.uiState.value.hasLoaded && !viewModel.uiState.value.isLoading }
    }

    private fun awaitLoadError() {
        compose.waitUntil(5_000) {
            !viewModel.uiState.value.hasLoaded && !viewModel.uiState.value.isLoading &&
                viewModel.uiState.value.errorMessage != null
        }
    }

    private fun awaitSaveError() {
        compose.waitUntil(5_000) { viewModel.uiState.value.hasLoaded && viewModel.uiState.value.errorMessage != null }
    }

    /** Only this test's isolated session directory is made unavailable; user sessions are untouched. */
    private fun blockDocumentDirectory(): File {
        val directory = File(context.filesDir, "session_documents")
        val parked = File(testRoot, "parked-session-documents")
        assertTrue(directory.renameTo(parked))
        directory.writeText("temporarily unavailable test storage")
        return parked
    }

    private fun restoreDocumentDirectory(parked: File) {
        val directory = File(context.filesDir, "session_documents")
        assertTrue(directory.isFile)
        assertTrue(directory.delete())
        assertTrue(parked.renameTo(directory))
    }

    private fun writePhoto(file: File) {
        file.parentFile!!.mkdirs()
        val bitmap = Bitmap.createBitmap(12, 16, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        } finally {
            bitmap.recycle()
        }
    }
}
