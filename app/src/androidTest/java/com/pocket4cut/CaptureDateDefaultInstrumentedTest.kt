package com.pocket4cut

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.InputSource
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.presentation.capture.CapturePhase
import com.pocket4cut.presentation.capture.CaptureViewModel
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.presentation.settings.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@RunWith(AndroidJUnit4::class)
class CaptureDateDefaultInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    private val preferenceNames = ConcurrentHashMap.newKeySet<String>()
    private lateinit var testRoot: File
    private lateinit var fixtureContext: Context
    private lateinit var fixtureApplication: Application
    private lateinit var sessions: SessionDocumentRepository
    private var originalProcessDateDefault = false

    @Before fun setUp() {
        val app = instrumentation.targetContext
        val namespace = "capture-date-${UUID.randomUUID()}"
        testRoot = File(app.cacheDir, namespace).apply { mkdirs() }
        fixtureContext = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File(testRoot, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(testRoot, "external/$type").apply { mkdirs() }

            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                val isolatedName = "${namespace}_$name"
                preferenceNames.add(isolatedName)
                return app.getSharedPreferences(isolatedName, mode)
            }
        }
        fixtureApplication = object : Application() {
            override fun getApplicationContext(): Context = fixtureContext
        }
        sessions = SessionDocumentRepository(fixtureContext)
        originalProcessDateDefault = AppSettings.showDateByDefault
    }

    @After fun tearDown() {
        instrumentation.runOnMainSync { store.clear() }
        val app = instrumentation.targetContext
        preferenceNames.forEach { app.deleteSharedPreferences(it) }
        val cache = app.cacheDir.canonicalFile
        assertTrue(testRoot.canonicalPath.startsWith(cache.path + File.separator))
        testRoot.deleteRecursively()
        assertEquals(originalProcessDateDefault, AppSettings.showDateByDefault)
    }

    @Test fun newCameraDraftUsesEnabledDateDefaultBeforeFirstCapture(): Unit = runBlocking {
        assertNewCameraDraftDateDefault(enabled = true)
    }

    @Test fun newCameraDraftUsesDisabledDateDefaultBeforeFirstCapture(): Unit = runBlocking {
        assertNewCameraDraftDateDefault(enabled = false)
    }

    @Test fun restoredCameraDraftKeepsItsDateChoiceAfterDefaultChanges(): Unit = runBlocking {
        for (savedShowDate in listOf(false, true)) {
            val original = sessions.create(SessionDocument(
                sessionId = UUID.randomUUID().toString(),
                createdAt = 1_700_000_000_000L,
                captureCount = FrameType.TWO_CUT.captureCount,
                selectedCount = FrameType.TWO_CUT.selectCount,
                draft = SessionDraft(showDate = savedShowDate, dateText = "2001.02.03"),
            ))
            setIsolatedDateDefault(!savedShowDate)
            val viewModel = newViewModel()
            instrumentation.runOnMainSync {
                viewModel.restoreSession(original.sessionId, FrameType.TWO_CUT)
            }
            val state = withTimeout(5_000) {
                viewModel.uiState.first {
                    it.sessionId == original.sessionId && it.phase == CapturePhase.PAUSED
                }
            }

            assertNull(state.errorMessage)
            assertEquals(0, state.currentShot)
            assertEquals(original, sessions.getById(original.sessionId))
            assertEquals(originalProcessDateDefault, AppSettings.showDateByDefault)
        }
    }

    private suspend fun assertNewCameraDraftDateDefault(enabled: Boolean) {
        setIsolatedDateDefault(enabled)
        val viewModel = newViewModel()
        instrumentation.runOnMainSync {
            viewModel.start(FrameType.TWO_CUT)
            // Pause in the same main-thread turn: retain creation, never start a camera request.
            viewModel.pause()
        }
        val state = withTimeout(5_000) {
            viewModel.uiState.first { it.sessionId != null && it.phase == CapturePhase.PAUSED }
        }
        val saved = sessions.getById(requireNotNull(state.sessionId))!!

        assertNull(state.errorMessage)
        assertEquals(InputSource.CAMERA, saved.inputSource)
        assertEquals(SessionStage.CAPTURE, saved.stage)
        assertEquals(enabled, saved.draft.showDate)
        assertEquals(
            SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(saved.createdAt)),
            saved.draft.dateText,
        )
        assertTrue(saved.photos.isEmpty())
        assertEquals(0, state.currentShot)
        assertEquals(originalProcessDateDefault, AppSettings.showDateByDefault)
    }

    private fun setIsolatedDateDefault(enabled: Boolean) {
        assertTrue(fixtureContext.getSharedPreferences("pocket4cut_settings", Context.MODE_PRIVATE)
            .edit().putBoolean("showDateByDefault", enabled).commit())
    }

    private fun newViewModel(): CaptureViewModel {
        lateinit var viewModel: CaptureViewModel
        instrumentation.runOnMainSync {
            store.clear()
            viewModel = ViewModelProvider(
                store,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        CaptureViewModel(fixtureApplication, SavedStateHandle()) as T
                },
            )[CaptureViewModel::class.java]
        }
        return viewModel
    }
}
