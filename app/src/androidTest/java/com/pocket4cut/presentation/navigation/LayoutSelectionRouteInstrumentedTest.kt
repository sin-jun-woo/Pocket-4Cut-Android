package com.pocket4cut.presentation.navigation

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.ui.theme.Pocket4CutTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class LayoutSelectionRouteInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var testRoot: File
    private lateinit var context: Context
    private lateinit var sessions: SessionDocumentRepository
    private val navigations = AtomicInteger()

    @Before fun setUp() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        testRoot = File(app.cacheDir, "layout-route-test-${UUID.randomUUID()}").apply { mkdirs() }
        context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = File(testRoot, "files").apply { mkdirs() }
            override fun getExternalFilesDir(type: String?): File =
                File(testRoot, "external/$type").apply { mkdirs() }
        }
        sessions = SessionDocumentRepository(context)
    }

    @After fun tearDown() {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.canonicalFile
        assertTrue(testRoot.canonicalPath.startsWith(cache.path + File.separator))
        testRoot.deleteRecursively()
    }

    @Test fun corruptDocumentShowsRetryAndReloadsWithoutChangingOriginals() {
        val document = createFixture()
        val metadata = documentFile(document.sessionId)
        val validBytes = metadata.readBytes()
        val originalHashes = document.photos.map { sha256(sessions.resolvePhotoPath(it)) }
        metadata.writeText("{broken-layout-fixture")
        val damagedHash = sha256(metadata)

        showRoute(document.sessionId)
        awaitText("작업을 불러오지 못했습니다.")
        compose.onNodeWithText("다시 시도").assertIsDisplayed()
        compose.onNodeWithText("작업 보관함 열기").assertIsDisplayed()
        compose.onNodeWithText("레이아웃 선택").assertDoesNotExist()
        assertEquals(0, navigations.get())
        assertEquals(damagedHash, sha256(metadata))
        assertEquals(originalHashes, document.photos.map { sha256(sessions.resolvePhotoPath(it)) })

        // Repair only this test's metadata fixture, then exercise the actual retry button.
        metadata.writeBytes(validBytes)
        compose.onNodeWithText("다시 시도").performClick()
        awaitText("레이아웃 선택")
        assertEquals(document, runBlocking { sessions.getById(document.sessionId) })
        assertEquals(originalHashes, document.photos.map { sha256(sessions.resolvePhotoPath(it)) })
    }

    @Test fun saveFailureBlocksNavigationAndCanBeRetriedWithoutChangingOriginals() {
        val document = createFixture()
        val metadata = documentFile(document.sessionId)
        val validBytes = metadata.readBytes()
        val originalHashes = document.photos.map { sha256(sessions.resolvePhotoPath(it)) }
        showRoute(document.sessionId)
        awaitText("레이아웃 선택")

        metadata.writeText("{broken-before-layout-save")
        val damagedHash = sha256(metadata)
        compose.onNodeWithText("세로 2컷 선택").performClick()
        awaitText("작업을 불러오지 못했습니다.")
        assertEquals(0, navigations.get())
        assertEquals(damagedHash, sha256(metadata))
        assertEquals(originalHashes, document.photos.map { sha256(sessions.resolvePhotoPath(it)) })

        metadata.writeBytes(validBytes)
        compose.onNodeWithText("다시 시도").performClick()
        awaitText("레이아웃 선택")
        compose.onNodeWithText("세로 2컷 선택").performClick()
        compose.waitUntil(5_000) { navigations.get() == 1 }
        val saved = runBlocking { sessions.getById(document.sessionId) }!!
        assertEquals(SessionStage.FRAME, saved.stage)
        assertEquals(FrameLayoutId.TWO_VERTICAL.name, saved.draft.layoutId)
        assertEquals(document.draft.selectedPhotoIdsInOrder, saved.draft.selectedPhotoIdsInOrder)
        assertEquals(originalHashes, document.photos.map { sha256(sessions.resolvePhotoPath(it)) })
    }

    @Test fun missingSelectedPhotoShowsErrorWithoutRewritingSessionOrOtherPhoto() {
        val document = createFixture(includeSecondPhoto = false)
        val metadataHash = sha256(documentFile(document.sessionId))
        val firstPhoto = sessions.resolvePhotoPath(document.photos.first())
        val firstHash = sha256(firstPhoto)

        showRoute(document.sessionId)
        awaitText("작업을 불러오지 못했습니다.")
        compose.onNodeWithText("선택한 사진 원본을 찾지 못했습니다. 작업 보관함에서 확인해 주세요.")
            .assertIsDisplayed()
        compose.onNodeWithText("레이아웃 선택").assertDoesNotExist()
        assertEquals(0, navigations.get())
        assertEquals(metadataHash, sha256(documentFile(document.sessionId)))
        assertEquals(firstHash, sha256(firstPhoto))
    }

    private fun createFixture(includeSecondPhoto: Boolean = true): SessionDocument = runBlocking {
        val id = UUID.randomUUID().toString()
        val refs = List(2) { index ->
            val path = "captures/$id/cap_0${index + 1}.jpg"
            if (index == 0 || includeSecondPhoto) {
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Pocket4Cut/$path")
                file.parentFile!!.mkdirs()
                val bitmap = Bitmap.createBitmap(16, 24, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(if (index == 0) Color.RED else Color.BLUE)
                }
                try {
                    file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
                } finally {
                    bitmap.recycle()
                }
            }
            PhotoRef(UUID.randomUUID().toString(), path, index)
        }
        sessions.create(SessionDocument(
            sessionId = id,
            createdAt = 1_700_000_000_000L,
            captureCount = 4,
            selectedCount = 2,
            frameTypeId = "2",
            stage = SessionStage.SELECT,
            photos = refs,
            draft = SessionDraft(selectedPhotoIdsInOrder = refs.map { it.photoId }),
        ))
    }

    private fun showRoute(sessionId: String) {
        compose.setContent {
            Pocket4CutTheme {
                LayoutSelectionRoute(
                    sessions = sessions,
                    sessionId = sessionId,
                    frameType = FrameType.TWO_CUT,
                    onLayoutSaved = { navigations.incrementAndGet() },
                    onCancel = {},
                    onOpenGallery = {},
                )
            }
        }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    private fun documentFile(id: String): File = File(context.filesDir, "session_documents/$id.json")

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256")
        .digest(file.readBytes()).joinToString("") { "%02x".format(it) }
}
