package com.pocket4cut

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocket4cut.core.util.BitmapDecoding
import com.pocket4cut.domain.model.PhotoCrop
import com.pocket4cut.frame.CropMath
import com.pocket4cut.frame.CropRect
import com.pocket4cut.frame.PhotoCropTransform
import com.pocket4cut.presentation.detailEdit.CropEditorScreen
import com.pocket4cut.ui.theme.Pocket4CutTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the screen's pointer/semantics callbacks, not persistence or navigation. */
@RunWith(AndroidJUnit4::class)
class CropEditorGestureInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private val editorVisible = mutableStateOf(true)
    private var fixture: Bitmap? = null

    @After
    fun releaseFixtureAfterComposition() {
        fixture?.let { bitmap ->
            compose.runOnIdle { editorVisible.value = false }
            compose.waitForIdle()
            bitmap.recycle()
        }
    }

    @Test
    fun pinchAndDragCommitFiniteClampedCropsWithoutExposingBlankEdges() {
        val callbacks = showEditor()
        val preview = compose.onNodeWithContentDescription("사진 자르기 미리보기")
            .assertIsDisplayed()

        preview.performTouchInput {
            pinch(
                start0 = Offset(width * 0.4f, centerY),
                end0 = Offset(width * 0.3f, centerY),
                start1 = Offset(width * 0.6f, centerY),
                end1 = Offset(width * 0.7f, centerY),
            )
        }
        val pinched = compose.runOnIdle {
            assertEquals(1, callbacks.commits.size)
            callbacks.commits.last().also {
                assertEquals(2f, it.zoom, 0.03f)
                assertEquals(callbacks.previews.last(), it)
            }
        }

        preview.performTouchInput {
            swipe(center, Offset(width * 0.75f, height * 0.75f))
        }
        compose.runOnIdle {
            assertEquals(2, callbacks.commits.size)
            val dragged = callbacks.commits.last()
            assertTrue("Dragging right must move the source focus left", dragged.focusX < pinched.focusX)
            assertTrue("Dragging down must move the source focus up", dragged.focusY < pinched.focusY)
            assertEquals(pinched.zoom, dragged.zoom, 0.001f)
            assertEquals(callbacks.previews.last(), dragged)
        }

        preview.performTouchInput {
            pinch(
                start0 = Offset(width * 0.45f, centerY),
                end0 = Offset(width * 0.1f, centerY),
                start1 = Offset(width * 0.55f, centerY),
                end1 = Offset(width * 0.9f, centerY),
            )
        }
        compose.runOnIdle { assertEquals(4f, callbacks.commits.last().zoom, 0.001f) }
        repeat(6) {
            preview.performTouchInput {
                swipe(center, Offset(width * 0.95f, height * 0.95f))
            }
        }
        val viewport = preview.fetchSemanticsNode().size.let {
            CropRect(0f, 0f, it.width.toFloat(), it.height.toFloat())
        }
        compose.runOnIdle {
            val edgeCrop = callbacks.commits.last()
            val scale = CropMath.aspectFillScale(320f, 240f, viewport.width, viewport.height) * 4f
            assertEquals(viewport.width / (2f * 320f * scale), edgeCrop.focusX, 0.001f)
            assertEquals(viewport.height / (2f * 240f * scale), edgeCrop.focusY, 0.001f)
        }

        preview.performTouchInput {
            pinch(
                start0 = Offset(width * 0.1f, centerY),
                end0 = Offset(width * 0.49f, centerY),
                start1 = Offset(width * 0.9f, centerY),
                end1 = Offset(width * 0.51f, centerY),
            )
        }
        compose.runOnIdle {
            assertEquals(1f, callbacks.commits.last().zoom, 0.001f)
            assertEquals(10, callbacks.commits.size)
            assertEquals(callbacks.previews.last(), callbacks.commits.last())
            (callbacks.previews + callbacks.commits).forEach { assertValidCoverage(it, viewport) }
        }
    }

    @Test
    fun labelledSliderDirectionButtonsCenterAndDoneCommitThroughTheUi() {
        val callbacks = showEditor()
        val slider = compose.onNodeWithContentDescription("사진 확대 비율").assertIsDisplayed()
        slider.performTouchInput {
            swipe(Offset(width * 0.1f, centerY), Offset(width - 1f, centerY))
        }
        compose.runOnIdle {
            assertTrue(callbacks.commits.isNotEmpty())
            assertEquals(4f, callbacks.commits.last().zoom, 0.001f)
            assertEquals(callbacks.previews.last(), callbacks.commits.last())
        }
        assertEquals(4f, slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current, 0.001f)

        compose.onNodeWithContentDescription("초점을 왼쪽으로 이동").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("초점을 위로 이동").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertTrue(callbacks.commits.last().focusX < 0.5f)
            assertTrue(callbacks.commits.last().focusY < 0.5f)
        }
        compose.onNodeWithContentDescription("초점을 오른쪽으로 이동").performClick()
        compose.onNodeWithContentDescription("초점을 아래로 이동").performClick()
        compose.runOnIdle {
            assertEquals(0.5f, callbacks.commits.last().focusX, 0.001f)
            assertEquals(0.5f, callbacks.commits.last().focusY, 0.001f)
        }

        compose.onNodeWithText("가운데 맞춤").assertIsDisplayed().performClick()
        val beforeDone = compose.runOnIdle {
            assertEquals(PhotoCrop(), callbacks.commits.last())
            assertEquals(callbacks.previews.last(), callbacks.commits.last())
            callbacks.commits.size
        }
        compose.onNodeWithText("완료").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(beforeDone + 1, callbacks.commits.size)
            assertEquals(PhotoCrop(), callbacks.commits.last())
            assertEquals(1, callbacks.dismissals)
        }
    }

    private fun showEditor(): Callbacks {
        val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.rgb(40, 100, 160))
            Canvas(it).drawRect(160f, 0f, 320f, 240f, Paint().apply { color = Color.rgb(210, 130, 60) })
        }
        fixture = bitmap
        val callbacks = Callbacks()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
                Box(Modifier.size(width = 360.dp, height = 640.dp)) {
                    Pocket4CutTheme {
                        if (editorVisible.value) {
                            CropEditorScreen(
                                bitmap = bitmap,
                                initialCrop = PhotoCrop(),
                                quarterTurnsClockwise = 0,
                                flipHorizontal = false,
                                slotAspectRatio = 3f / 4f,
                                sourceDimensions = BitmapDecoding.ImageDimensions(8_000, 6_000),
                                onPreview = { callbacks.previews += it },
                                onCommit = { callbacks.commits += it },
                                onDismiss = { callbacks.dismissals += 1 },
                            )
                        }
                    }
                }
            }
        }
        return callbacks
    }

    private fun assertValidCoverage(crop: PhotoCrop, viewport: CropRect) {
        assertTrue("Gesture emitted invalid crop: $crop", CropMath.isValid(crop))
        val clamped = CropMath.clampCrop(crop, 320f, 240f, viewport.width, viewport.height)
        assertEquals(clamped.focusX, crop.focusX, 0.001f)
        assertEquals(clamped.focusY, crop.focusY, 0.001f)
        val transform = PhotoCropTransform(crop)
        val drawn = CropMath.drawRect(320f, 240f, viewport, transform)
        assertTrue(drawn.left <= 0.01f && drawn.top <= 0.01f)
        assertTrue(drawn.right >= viewport.right - 0.01f && drawn.bottom >= viewport.bottom - 0.01f)
        val visible = CropMath.visibleSourceRect(320f, 240f, viewport, transform)
        assertTrue(listOf(visible.left, visible.top, visible.right, visible.bottom).all { it.isFinite() && it in 0f..1f })
        assertTrue(visible.width > 0f && visible.height > 0f)
    }

    private class Callbacks {
        val previews = mutableListOf<PhotoCrop>()
        val commits = mutableListOf<PhotoCrop>()
        var dismissals = 0
    }
}
