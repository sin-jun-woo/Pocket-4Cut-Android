package com.pocket4cut

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.frame.CollageLayoutMath
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.frame.RenderSnapshot
import com.pocket4cut.frame.SeasonBackgroundFrameFactory
import com.pocket4cut.presentation.edit.EditViewModel
import com.pocket4cut.presentation.edit.PendingCollageStore
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.theme.Season
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeasonalLegacyPreviewInstrumentedTest {
    @Test fun restoringSeasonalSixCutDraftKeepsLegacyGeometryInGeneralEditorAndExport() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as Application
        assertEquals("Synthetic session must stay in the QA app", "com.pocket4cut.qa", app.packageName)
        val sessionId = "seasonal-legacy-${UUID.randomUUID()}"
        val photoIds = List(6) { UUID.randomUUID().toString() }
        val sessions = SessionDocumentRepository(app)
        val storage = FileImageStorage(app)
        val store = ViewModelStore()
        val theme = FrameCatalog.themes(FrameType.SIX_CUT).first()
        val style = FrameLayouts.byId(FrameLayoutId.SIX_COLLAGE)
        val fixture = Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(91, 131, 172))
        }
        var sessionCreated = false
        try {
            runBlocking {
                val bytes = ByteArrayOutputStream().use { output ->
                    assertTrue(fixture.compress(Bitmap.CompressFormat.JPEG, 95, output))
                    output.toByteArray()
                }
                for (index in photoIds.indices) storage.saveCapture(bytes, sessionId, index + 1)
                sessions.create(SessionDocument(
                    sessionId = sessionId,
                    createdAt = System.currentTimeMillis(),
                    captureCount = 10,
                    selectedCount = 6,
                    photos = photoIds.mapIndexed { index, id ->
                        PhotoRef(id, "captures/$sessionId/cap_0${index + 1}.jpg", index)
                    },
                    draft = SessionDraft(
                        selectedPhotoIdsInOrder = photoIds,
                        layoutId = style.id.name,
                        layoutVersion = 1,
                        themeId = theme.id,
                        backgroundType = "season",
                        seasonId = "autumn",
                        customDesignJson = PendingCollageStore.serializeDesign(
                            SeasonBackgroundFrameFactory.design(Season.AUTUMN),
                        ),
                    ),
                ))
                sessionCreated = true
            }
            lateinit var viewModel: EditViewModel
            instrumentation.runOnMainSync {
                viewModel = ViewModelProvider(
                    store, ViewModelProvider.AndroidViewModelFactory(app),
                )[EditViewModel::class.java]
                viewModel.init(FrameType.SIX_CUT, sessionId, photoIds.indices.toList(), style.id)
            }
            val deadline = SystemClock.uptimeMillis() + 10_000L
            while (viewModel.uiState.value.isLoading && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(10)
            }
            val ui = viewModel.uiState.value
            assertNull("Legacy draft must load successfully", ui.errorMessage)
            assertEquals(6, ui.orderedImages.size)
            assertEquals("General preview must not silently default to version 2", 1, ui.layoutVersion)
            assertEquals(Season.AUTUMN, ui.customFrameDesign?.resolvedSeason)
            val snapshot = runBlocking {
                RenderSnapshot.from(requireNotNull(sessions.getById(sessionId)), sessions, FrameType.SIX_CUT)
            }
            assertEquals(ui.layoutVersion, snapshot.layoutVersion)
            assertEquals(photoIds, snapshot.photoIdsInOrder)
            val preview = CollageLayoutMath.computeForPreview(style, theme, null, 390f, ui.layoutVersion)
            val export = CollageLayoutMath.compute(style, theme, null, null, 390f, snapshot.layoutVersion)
            assertEquals(export.cells, preview.cells)
            assertTrue("Legacy layout remains six equal slots", preview.cells.all {
                it.width() == preview.cells.first().width() && it.height() == preview.cells.first().height()
            })
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            if (sessionCreated) runBlocking { sessions.requestDelete(sessionId) }
            fixture.recycle()
        }
    }
}
