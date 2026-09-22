package com.pocket4cut.frame

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kotlin.math.min
import kotlin.math.roundToInt

/** The preview uses the same Android Canvas painter, geometry, and layer order as JPEG output. */
@Composable
fun CollagePreview(
    images: List<Bitmap>,
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
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        // bottomCaption reserves geometry and supports legacy callers. A date-only caption
        // must not also be drawn as user text, or the date appears twice.
        val caption = captionTextPart ?: bottomCaption?.takeIf { captionDatePart == null }
        val dimensions = remember(containerPx, frameStyle, theme, bottomCaption, layoutVersion) {
            CollageLayoutMath.computeForPreview(
                frameStyle, theme, bottomCaption, containerPx, layoutVersion,
            )
        }
        val input = CollageRenderer.Input(
            images = images.take(frameType.selectCount),
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
        )
        Canvas(
            modifier = Modifier
                .width(with(density) { dimensions.canvasWidth.toDp() })
                .height(with(density) { dimensions.canvasHeight.toDp() }),
        ) {
            drawIntoCanvas { target ->
                CollageRenderer.drawScene(target.nativeCanvas, input, dimensions)
            }
        }
    }
}

@Composable
fun CollagePreviewScaledToFit(
    images: List<Bitmap>,
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
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val maxW = constraints.maxWidth
        val maxH = constraints.maxHeight
        if (maxW == 0) return@SubcomposeLayout layout(0, 0) {}
        val innerConstraints = Constraints(maxWidth = maxW, maxHeight = Constraints.Infinity)
        val placeable = subcompose("collagePreview") {
            CollagePreview(
                images = images,
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
