package com.pocket4cut

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.importing.PhotoImportRepository
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.presentation.photoImport.PhotoImportViewModel
import com.pocket4cut.presentation.settings.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import java.util.concurrent.ConcurrentHashMap

@RunWith(AndroidJUnit4::class)
class AlbumDateDefaultInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val stores = mutableListOf<ViewModelStore>()
    private val preferenceNames = ConcurrentHashMap.newKeySet<String>()
    private lateinit var root: File
    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var sessions: SessionDocumentRepository
    private var originalProcessDateDefault = false

    @Before fun setUp() {
        val base = instrumentation.targetContext
        val namespace = "album-date-default-${UUID.randomUUID()}"
        root = File(base.cacheDir, namespace).apply { mkdirs() }
        context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(root, "external/$type").apply { mkdirs() }

            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                val isolatedName = "$namespace-$name"
                preferenceNames += isolatedName
                return base.getSharedPreferences(isolatedName, mode)
            }
        }
        application = object : Application() {
            override fun getApplicationContext(): Context = context
        }
        sessions = SessionDocumentRepository(context)
        originalProcessDateDefault = AppSettings.showDateByDefault
    }

    @After fun tearDown() {
        instrumentation.runOnMainSync { stores.forEach(ViewModelStore::clear) }
        val base = instrumentation.targetContext
        preferenceNames.forEach { base.deleteSharedPreferences(it) }
        assertTrue(root.canonicalPath.startsWith(base.cacheDir.canonicalPath + File.separator))
        root.deleteRecursively()
        assertEquals(originalProcessDateDefault, AppSettings.showDateByDefault)
    }

    @Test fun newAlbumDraftUsesSavedDateDefaultAndFurtherImportsKeepItsChoice(): Unit = runBlocking {
        for (enabled in listOf(false, true)) {
            setDateDefault(enabled)
            val sessionId = UUID.randomUUID().toString()
            val viewModel = createViewModel(sessionId)
            withTimeout(5_000) { viewModel.uiState.first { it.isInitialized } }
            val firstSource = jpeg("$sessionId-first.jpg")
            instrumentation.runOnMainSync { viewModel.importUris(listOf(Uri.fromFile(firstSource))) }
            withTimeout(5_000) { viewModel.uiState.first { !it.isBusy && it.photos.size == 1 } }

            val created = sessions.getCurrentById(sessionId)!!
            assertEquals(enabled, created.draft.showDate)
            assertTrue(created.draft.dateText.isNotBlank())
            assertEquals(enabled, journal(sessionId, created.photos.single().photoId).getBoolean("showDateByDefault"))

            // A changed global default is only for new work, not an existing album draft.
            setDateDefault(!enabled)
            val secondSource = jpeg("$sessionId-second.jpg")
            instrumentation.runOnMainSync { viewModel.importUris(listOf(Uri.fromFile(secondSource))) }
            withTimeout(5_000) { viewModel.uiState.first { !it.isBusy && it.photos.size == 2 } }
            val continued = sessions.getCurrentById(sessionId)!!
            assertEquals(enabled, continued.draft.showDate)
            assertEquals(created.draft.dateText, continued.draft.dateText)
            assertTrue(firstSource.isFile && secondSource.isFile)
        }
    }

    @Test fun firstPhotoPublicationRecoveryKeepsRecordedDateDefault(): Unit = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        val source = jpeg("published-source.jpg")
        val before = source.readBytes()
        val blocker = File(context.filesDir, "session_documents").apply { writeText("isolated write failure") }
        val failed = PhotoImportRepository(context, sessions).importUris(
            sessionId, FrameType.SIX_CUT.id, listOf(Uri.fromFile(source)), showDateByDefault = true,
        )
        assertTrue(failed.failures.isNotEmpty())
        val journalFile = File(context.filesDir, "import_journals/$sessionId").listFiles()!!.single()
        val published = JSONObject(journalFile.readText())
        assertEquals("FILE_PUBLISHED", published.getString("status"))
        assertTrue(published.getBoolean("showDateByDefault"))
        assertTrue(blocker.delete())

        setDateDefault(false)
        val recovery = PhotoImportRepository(context, SessionDocumentRepository(context)).recover(sessionId)
        assertTrue(recovery.failures.isEmpty())
        assertNotNull(recovery.session)
        assertTrue(recovery.session!!.draft.showDate)
        assertTrue(recovery.session!!.draft.dateText.isNotBlank())
        assertEquals(1, recovery.session!!.photos.size)
        assertTrue(before.contentEquals(source.readBytes()))
        val repeated = PhotoImportRepository(context).recover(sessionId)
        assertTrue(repeated.session!!.draft.showDate)
        assertEquals(1, repeated.session!!.photos.size)
    }

    @Test fun legacyImportJournalWithoutDateDefaultKeepsPreviousOffBehavior(): Unit = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        val source = jpeg("legacy-journal-source.jpg")
        val blocker = File(context.filesDir, "session_documents").apply { writeText("isolated write failure") }
        val failed = PhotoImportRepository(context, sessions).importUris(
            sessionId, FrameType.TWO_CUT.id, listOf(Uri.fromFile(source)), showDateByDefault = true,
        )
        assertTrue(failed.failures.isNotEmpty())
        val journalFile = File(context.filesDir, "import_journals/$sessionId").listFiles()!!.single()
        val legacy = JSONObject(journalFile.readText()).apply { remove("showDateByDefault") }
        journalFile.writeText(legacy.toString())
        assertTrue(blocker.delete())

        setDateDefault(true)
        val recovery = PhotoImportRepository(context).recover(sessionId)
        assertTrue(recovery.failures.isEmpty())
        assertFalse(recovery.session!!.draft.showDate)
        assertTrue(source.isFile)
    }

    private fun createViewModel(sessionId: String): PhotoImportViewModel {
        lateinit var model: PhotoImportViewModel
        instrumentation.runOnMainSync {
            val store = ViewModelStore().also(stores::add)
            model = ViewModelProvider(store, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PhotoImportViewModel(application, FrameType.SIX_CUT, sessionId) as T
            })[PhotoImportViewModel::class.java]
        }
        return model
    }

    private fun setDateDefault(enabled: Boolean) {
        assertTrue(context.getSharedPreferences("pocket4cut_settings", Context.MODE_PRIVATE)
            .edit().putBoolean("showDateByDefault", enabled).commit())
    }

    private fun journal(sessionId: String, photoId: String): JSONObject = JSONObject(
        File(context.filesDir, "import_journals/$sessionId/$photoId.json").readText(),
    )

    private fun jpeg(name: String): File = File(root, name).also { file ->
        val image = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        try {
            image.eraseColor(android.graphics.Color.CYAN)
            file.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.JPEG, 95, it)) }
        } finally {
            image.recycle()
        }
    }
}
