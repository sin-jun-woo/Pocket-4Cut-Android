package com.pocket4cut.frame

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.frame.occasion.OccasionTheme
import com.pocket4cut.frame.rendering.OccasionArtwork
import com.pocket4cut.frame.rendering.SeasonalStickerArt
import com.pocket4cut.ui.designsystem.theme.Season
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

private const val OCCASION_PREVIEW_DEBOUNCE_MS = 80L
internal const val OCCASION_PREVIEW_MAX_CONCURRENT_DECODES = 2
private val occasionPreviewDecodeSemaphore = Semaphore(OCCASION_PREVIEW_MAX_CONCURRENT_DECODES)

internal suspend fun <T> loadOccasionArtworkForPreview(
    debounceMillis: Long = OCCASION_PREVIEW_DEBOUNCE_MS,
    loader: () -> Result<T>,
): Result<T> {
    if (debounceMillis > 0L) delay(debounceMillis)
    currentCoroutineContext().ensureActive()
    val result = occasionPreviewDecodeSemaphore.withPermit {
        currentCoroutineContext().ensureActive()
        withContext(Dispatchers.IO) {
            currentCoroutineContext().ensureActive()
            loader().also { currentCoroutineContext().ensureActive() }
        }
    }
    currentCoroutineContext().ensureActive()
    return result
}

/** The preview uses the same Android Canvas painter, geometry, and layer order as JPEG output. */
@Composable
fun CollagePreview(
    images: List<Bitmap>,
    cropTransforms: List<PhotoCropTransform> = emptyList(),
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    modifier: Modifier = Modifier,
    filterId: FilterId = FilterId.ORIGINAL,
    overrideBackground: Color? = null,
    backgroundGradient: List<Color>? = null,
    overrideBackgroundImage: Bitmap? = null,
    customFrameDesign: CustomFrameDesign? = null,
    customDecorations: List<CustomFrameDecoration> = emptyList(),
    bottomCaption: String? = null,
    captionTextPart: String? = null,
    captionDatePart: String? = null,
    captionTextSizePt: Float = 16f,
    captionDateSizePt: Float = 16f,
    captionFontName: String? = null,
    captionColorRGB: Long? = null,
    layoutVersion: Int = 2,
    occasionTheme: OccasionTheme? = null,
    onOccasionArtworkReadyChanged: ((themeId: String, ready: Boolean) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val requestedSeason = customFrameDesign?.resolvedSeason
    var artworkRetry by remember(requestedSeason, occasionTheme?.id) { mutableIntStateOf(0) }
    val artworkState by produceState<Pair<Season, Result<SeasonalStickerArt.Sheet>>?>(
        initialValue = null, requestedSeason, artworkRetry,
    ) {
        value = null
        if (requestedSeason != null) {
            val result = try {
                Result.success(withContext(Dispatchers.IO) {
                    SeasonalStickerArt.load(context.applicationContext, requestedSeason)
                })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
            value = requestedSeason to result
        }
    }
    val artworkResult = artworkState?.takeIf { it.first == requestedSeason }?.second
    val seasonalArt = artworkResult?.getOrNull()
    val requestedOccasionId = occasionTheme?.id
    val occasionArtworkState by produceState<Pair<String, Result<OccasionArtwork.Sheet>>?>(
        initialValue = null,
        requestedOccasionId,
        occasionTheme?.atlas?.assetPath,
        artworkRetry,
    ) {
        value = null
        val requestedTheme = occasionTheme
        if (requestedTheme != null) {
            val previewJob = currentCoroutineContext()[Job]
            val result = try {
                loadOccasionArtworkForPreview {
                    OccasionArtwork.loadCatching(
                        context = context.applicationContext,
                        themeId = requestedTheme.id,
                        assetPath = requestedTheme.atlas.assetPath,
                        expectedWidth = requestedTheme.atlas.width,
                        expectedHeight = requestedTheme.atlas.height,
                        canPublish = { previewJob?.isActive != false },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
            value = requestedTheme.id to result
        }
    }
    val occasionArtworkResult = occasionArtworkState
        ?.takeIf { it.first == requestedOccasionId }
        ?.second
    val occasionArtwork = occasionArtworkResult?.getOrNull()
    LaunchedEffect(requestedOccasionId, occasionArtworkResult) {
        requestedOccasionId?.let { id ->
            onOccasionArtworkReadyChanged?.invoke(id, occasionArtworkResult?.isSuccess == true)
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        // bottomCaption reserves geometry and supports legacy callers. A date-only caption
        // must not also be drawn as user text, or the date appears twice.
        val caption = captionTextPart ?: bottomCaption?.takeIf { captionDatePart == null }
        val hasCaption = !caption.isNullOrBlank() || !captionDatePart.isNullOrBlank()
        val dimensions = remember(containerPx, frameStyle, theme, hasCaption, layoutVersion) {
            CollageLayoutMath.computeForPreview(
                frameStyle, theme, if (hasCaption) "caption" else null, containerPx, layoutVersion,
            )
        }
        val input = CollageRenderer.Input(
            images = images.take(frameType.selectCount),
            cropTransforms = cropTransforms.take(frameType.selectCount),
            frameStyle = frameStyle,
            theme = theme,
            overrideBackground = overrideBackground,
            backgroundGradient = backgroundGradient,
            overrideBackgroundImage = overrideBackgroundImage,
            customDecorations = customDecorations,
            customFrameDesign = customFrameDesign,
            filterId = filterId,
            text = caption?.takeIf { it.isNotBlank() },
            dateString = captionDatePart,
            textFontSize = captionTextSizePt,
            dateFontSize = captionDateSizePt,
            textColorRGB = captionColorRGB,
            captionFontName = captionFontName,
            context = context,
            layoutVersion = layoutVersion,
            seasonalArt = seasonalArt,
            occasionTheme = occasionTheme,
            occasionArtwork = occasionArtwork,
        )
        val canvasModifier = Modifier
            .width(with(density) { dimensions.canvasWidth.toDp() })
            .height(with(density) { dimensions.canvasHeight.toDp() })
        val seasonalPending = requestedSeason != null && seasonalArt == null
        val occasionPending = requestedOccasionId != null && occasionArtwork == null
        if (seasonalPending || occasionPending) {
            Box(canvasModifier, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val failed = artworkResult?.isFailure == true || occasionArtworkResult?.isFailure == true
                    Text(if (failed) "프레임을 불러오지 못했습니다." else "프레임을 불러오는 중입니다.")
                    if (failed) {
                        TextButton(onClick = { artworkRetry++ }) { Text("다시 시도") }
                    }
                }
            }
        } else {
            Canvas(modifier = canvasModifier) {
                drawIntoCanvas { target ->
                    CollageRenderer.drawScene(target.nativeCanvas, input, dimensions)
                }
            }
        }
    }
}

@Composable
fun CollagePreviewScaledToFit(
    images: List<Bitmap>,
    cropTransforms: List<PhotoCropTransform> = emptyList(),
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    modifier: Modifier = Modifier,
    filterId: FilterId = FilterId.ORIGINAL,
    overrideBackground: Color? = null,
    backgroundGradient: List<Color>? = null,
    overrideBackgroundImage: Bitmap? = null,
    customFrameDesign: CustomFrameDesign? = null,
    customDecorations: List<CustomFrameDecoration> = emptyList(),
    bottomCaption: String? = null,
    captionTextPart: String? = null,
    captionDatePart: String? = null,
    captionTextSizePt: Float = 16f,
    captionDateSizePt: Float = 16f,
    captionFontName: String? = null,
    captionColorRGB: Long? = null,
    layoutVersion: Int = 2,
    occasionTheme: OccasionTheme? = null,
    onOccasionArtworkReadyChanged: ((themeId: String, ready: Boolean) -> Unit)? = null,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val maxW = constraints.maxWidth
        val maxH = constraints.maxHeight
        if (maxW == 0) return@SubcomposeLayout layout(0, 0) {}
        val innerConstraints = Constraints(maxWidth = maxW, maxHeight = Constraints.Infinity)
        val placeable = subcompose("collagePreview") {
            CollagePreview(
                images = images,
                cropTransforms = cropTransforms,
                frameType = frameType,
                frameStyle = frameStyle,
                theme = theme,
                filterId = filterId,
                overrideBackground = overrideBackground,
                backgroundGradient = backgroundGradient,
                overrideBackgroundImage = overrideBackgroundImage,
                customFrameDesign = customFrameDesign,
                customDecorations = customDecorations,
                bottomCaption = bottomCaption,
                captionTextPart = captionTextPart,
                captionDatePart = captionDatePart,
                captionTextSizePt = captionTextSizePt,
                captionDateSizePt = captionDateSizePt,
                captionFontName = captionFontName,
                captionColorRGB = captionColorRGB,
                layoutVersion = layoutVersion,
                occasionTheme = occasionTheme,
                onOccasionArtworkReadyChanged = onOccasionArtworkReadyChanged,
                modifier = Modifier.fillMaxWidth(),
            )
        }[0].measure(innerConstraints)
        val pw = placeable.width.toFloat().coerceAtLeast(1f)
        val ph = placeable.height.toFloat().coerceAtLeast(1f)
        val scaleW = maxW.toFloat() / pw
        val scaleH = if (maxH != Constraints.Infinity) maxH.toFloat() / ph else Float.POSITIVE_INFINITY
        val scale = min(min(scaleW, scaleH), 1f).coerceAtLeast(0f)
        val scaledW = (pw * scale).roundToInt().coerceAtLeast(1)
        val scaledH = (ph * scale).roundToInt().coerceAtLeast(1)
        val layoutW = if (constraints.hasBoundedWidth) constraints.maxWidth
            else scaledW.coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutH = if (constraints.hasBoundedHeight) constraints.maxHeight
            else scaledH.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(layoutW, layoutH) {
            val x = ((layoutW - scaledW) / 2f).roundToInt()
            val y = ((layoutH - scaledH) / 2f).roundToInt()
            placeable.placeWithLayer(x, y) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}
