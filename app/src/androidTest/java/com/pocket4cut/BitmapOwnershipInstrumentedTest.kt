package com.pocket4cut

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.CollageLayoutMath
import com.pocket4cut.frame.CollageRenderer
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.FrameColors
import com.pocket4cut.frame.FrameLayoutId
import com.pocket4cut.frame.FrameLayouts
import com.pocket4cut.presentation.detailEdit.DetailEditViewModel
import com.pocket4cut.presentation.edit.EditViewModel
import com.pocket4cut.presentation.navigation.FrameType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class BitmapOwnershipInstrumentedTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as Application

    @Test
    fun detailPreviewRemainsDrawableAfterReplacementResetAndViewModelClear() {
        val store = ViewModelStore()
        val baseImages = fixtures(2)
        val retainedPreviews = mutableListOf<Bitmap>()
        try {
            lateinit var viewModel: DetailEditViewModel
            instrumentation.runOnMainSync {
                viewModel = ViewModelProvider(
                    store,
                    ViewModelProvider.AndroidViewModelFactory(app),
                )[DetailEditViewModel::class.java]
                viewModel.initialize(
                    baseImages = baseImages,
                    imagePaths = emptyList(),
                    photoIds = listOf("photo-0", "photo-1"),
                    initialAdjustments = emptyMap(),
                    layoutVersion = 2,
                    dateText = "2026.09.22",
                    frameType = FrameType.TWO_CUT,
                    frameStyle = FrameLayouts.byId(FrameLayoutId.TWO_HORIZONTAL),
                    theme = FrameCatalog.themes(FrameType.TWO_CUT).first(),
                    frameColor = FrameColors.byId("white"),
                    globalFilter = FilterId.ORIGINAL,
                    customText = "",
                    showDate = false,
                    sessionId = "bitmap-lifetime-in-memory",
                    selectedIndexes = listOf(0, 1),
                )
            }

            awaitCondition("initial detail previews") {
                viewModel.uiState.value.collagePreviewImages.size == 2
            }
            val beforeSlotReplacement = viewModel.uiState.value.collagePreviewImages.first()
            retainedPreviews += beforeSlotReplacement

            instrumentation.runOnMainSync { viewModel.setBrightness(0.1f) }
            awaitCondition("slot preview replacement") {
                viewModel.uiState.value.collagePreviewImages.firstOrNull() !== beforeSlotReplacement
            }
            assertDrawable(beforeSlotReplacement)

            val beforeFullReplacement = viewModel.uiState.value.collagePreviewImages
            retainedPreviews += beforeFullReplacement
            instrumentation.runOnMainSync { viewModel.resetAllSlots() }
            awaitCondition("full preview replacement") {
                viewModel.uiState.value.collagePreviewImages.firstOrNull() !== beforeFullReplacement.first()
            }
            beforeFullReplacement.forEach(::assertDrawable)

            val beforeClear = viewModel.uiState.value.collagePreviewImages
            retainedPreviews += beforeClear
            instrumentation.runOnMainSync { store.clear() }
            beforeClear.forEach(::assertDrawable)
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            (retainedPreviews + baseImages).forEach { bitmap ->
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    @Test
    fun editFilterSwitchReusesOriginalsAndSceneAppliesLatestFilter() {
        val sessionId = "bitmap-lifetime-${System.nanoTime()}"
        val storage = FileImageStorage(app)
        val sessionStore = SessionDocumentRepository(app)
        val fixtures = fixtures(2)
        val photoIds = listOf(java.util.UUID.randomUUID().toString(), java.util.UUID.randomUUID().toString())
        val store = ViewModelStore()
        val retained = mutableListOf<Bitmap>()
        try {
            runBlocking {
                fixtures.forEachIndexed { index, bitmap ->
                    val bytes = ByteArrayOutputStream().use { output ->
                        assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
                        output.toByteArray()
                    }
                    storage.saveCapture(bytes, sessionId, index + 1)
                }
                sessionStore.create(SessionDocument(
                    sessionId = sessionId,
                    createdAt = System.currentTimeMillis(),
                    captureCount = 4,
                    selectedCount = 2,
                    photos = photoIds.mapIndexed { index, id ->
                        PhotoRef(id, "captures/$sessionId/cap_0${index + 1}.jpg", index)
                    },
                    draft = SessionDraft(selectedPhotoIdsInOrder = photoIds),
                ))
            }
            lateinit var viewModel: EditViewModel
            instrumentation.runOnMainSync {
                viewModel = ViewModelProvider(
                    store,
                    ViewModelProvider.AndroidViewModelFactory(app),
                )[EditViewModel::class.java]
                viewModel.init(
                    frameType = FrameType.TWO_CUT,
                    sessionId = sessionId,
                    selectedIndexes = listOf(0, 1),
                    frameLayoutId = FrameLayoutId.TWO_HORIZONTAL,
                )
            }
            awaitCondition("edit original image and filter thumbnails") {
                viewModel.uiState.value.orderedImages.size == 2 &&
                    viewModel.uiState.value.filterChipThumbnails.size == FilterId.entries.size
            }
            val retainedImage = viewModel.uiState.value.orderedImages.first()
            retained += retainedImage
            instrumentation.runOnMainSync {
                viewModel.setFilter(FilterId.SOFT)
                viewModel.setFilter(FilterId.FILM)
                viewModel.setFilter(FilterId.BW)
                viewModel.setFilter(FilterId.ORIGINAL)
                viewModel.setFilter(FilterId.FILM)
            }
            val state = viewModel.uiState.value
            assertTrue(state.selectedFilter == FilterId.FILM)
            assertTrue(state.orderedImages.first() === retainedImage)
            val style = FrameLayouts.byId(FrameLayoutId.TWO_HORIZONTAL)
            val theme = FrameCatalog.themes(FrameType.TWO_CUT).first()
            val original = CollageRenderer.render(
                state.orderedImages, style, theme, null, FilterId.ORIGINAL, null, null, 390,
            )
            val selected = CollageRenderer.render(
                state.orderedImages, style, theme, null, state.selectedFilter, null, null, 390,
            )
            try {
                val cell = CollageLayoutMath.compute(style, theme, null, null, 390f).cells.first()
                val x = cell.centerX().toInt()
                val y = cell.centerY().toInt()
                assertTrue("Latest filter must affect common-scene output", original.getPixel(x, y) != selected.getPixel(x, y))
            } finally {
                original.recycle()
                selected.recycle()
            }
            instrumentation.runOnMainSync { store.clear() }
            retained.forEach(::assertDrawable)
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            runBlocking { sessionStore.requestDelete(sessionId) }
            fixtures.forEach { if (!it.isRecycled) it.recycle() }
            retained.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    private fun fixtures(count: Int): List<Bitmap> = List(count) { index ->
        Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(if (index == 0) Color.RED else Color.BLUE)
        }
    }

    private fun assertDrawable(bitmap: Bitmap) {
        assertFalse("UI-retained bitmap was recycled", bitmap.isRecycled)
        val target = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        try {
            Canvas(target).drawBitmap(bitmap, null, android.graphics.Rect(0, 0, 1, 1), null)
        } finally {
            target.recycle()
        }
    }

    private fun awaitCondition(label: String, predicate: () -> Boolean) {
        val deadline = android.os.SystemClock.uptimeMillis() + 5_000
        while (!predicate() && android.os.SystemClock.uptimeMillis() < deadline) {
            android.os.SystemClock.sleep(10)
        }
        assertTrue("Timed out waiting for $label", predicate())
    }
}
