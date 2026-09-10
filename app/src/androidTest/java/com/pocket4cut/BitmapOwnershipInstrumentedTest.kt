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
import com.pocket4cut.frame.FilterId
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
    fun editOriginalRemainsDrawableAfterViewModelClear() {
        val sessionId = "bitmap-lifetime-${System.nanoTime()}"
        val storage = FileImageStorage(app)
        val fixture = fixtures(1).single()
        val bytes = ByteArrayOutputStream().use { output ->
            assertTrue(fixture.compress(Bitmap.CompressFormat.JPEG, 95, output))
            output.toByteArray()
        }
        val store = ViewModelStore()
        var retained: Bitmap? = null
        try {
            runBlocking { storage.saveCapture(bytes, sessionId, 1) }
            lateinit var viewModel: EditViewModel
            instrumentation.runOnMainSync {
                viewModel = ViewModelProvider(
                    store,
                    ViewModelProvider.AndroidViewModelFactory(app),
                )[EditViewModel::class.java]
                viewModel.init(
                    frameType = FrameType.TWO_CUT,
                    sessionId = sessionId,
                    selectedIndexes = listOf(0),
                    frameLayoutId = FrameLayoutId.TWO_HORIZONTAL,
                )
            }
            awaitCondition("edit original image and filter thumbnails") {
                viewModel.uiState.value.orderedImages.size == 1 &&
                    viewModel.uiState.value.filterChipThumbnails.size == FilterId.entries.size
            }
            val retainedImage = viewModel.uiState.value.orderedImages.single()
            retained = retainedImage
            instrumentation.runOnMainSync { store.clear() }
            assertDrawable(retainedImage)
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            runBlocking { storage.deleteSessionFiles(sessionId) }
            if (!fixture.isRecycled) fixture.recycle()
            retained?.takeIf { !it.isRecycled }?.recycle()
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
