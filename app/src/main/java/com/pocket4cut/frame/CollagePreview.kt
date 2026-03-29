package com.pocket4cut.frame

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocket4cut.presentation.navigation.FrameType
import kotlin.math.min
import kotlin.math.roundToInt

private const val BrandTitle = "Pocket 4Cut"

/**
 * [CollageLayoutMath] / [CollageRenderer]와 동일한 기하로 미리보기 (WYSIWYG).
 */
@Composable
fun CollagePreview(
    images: List<Bitmap>,
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    overrideBackground: Color? = null,
    bottomCaption: String? = null,
    modifier: Modifier = Modifier,
) {
    val effectiveBackground = overrideBackground ?: theme.background
    val used = images.take(frameType.selectCount)
    val brandColor = brandTextColor(effectiveBackground)
    val cellOverlay = cellPlaceholderOverlay(effectiveBackground)
    val shape = RoundedCornerShape(theme.cornerRadius.dp)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val containerPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val dim = remember(containerPx, frameStyle, theme, bottomCaption) {
            CollageLayoutMath.computeForPreview(frameStyle, theme, bottomCaption, containerPx)
        }

        val brandFontSp = with(density) { (BRAND_TITLE_TEXT_PT * dim.scale).toSp() }
        val captionSp = with(density) { (16f * dim.scale).toSp() }

        Column(
            modifier = Modifier
                .width(with(density) { dim.canvasWidth.toDp() })
                .height(with(density) { dim.canvasHeight.toDp() })
                .clip(shape)
                .background(effectiveBackground, shape)
                .border(theme.borderWidth.dp, theme.border, shape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { dim.headerArea.height().toDp() }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = BrandTitle,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = brandFontSp,
                        color = brandColor,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }

            val cols = frameStyle.columns
            val rows = frameStyle.rows
            val hGapPx = if (cols > 1) dim.cells[1].left - dim.cells[0].right else 0f
            val vGapPx = if (rows > 1) dim.cells[cols].top - dim.cells[0].bottom else 0f
            val hGap = with(density) { hGapPx.toDp() }
            val vGap = with(density) { vGapPx.toDp() }
            val sidePad = with(density) { dim.cells[0].left.toDp() }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sidePad),
                verticalArrangement = Arrangement.spacedBy(vGap),
            ) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(hGap),
                    ) {
                        for (c in 0 until cols) {
                            val idx = r * cols + c
                            val rect = dim.cells.getOrNull(idx) ?: continue
                            CollagePreviewCell(
                                bitmap = used.getOrNull(idx),
                                cellOverlay = cellOverlay,
                                iconTint = brandColor.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .size(
                                        width = with(density) { rect.width().toDp() },
                                        height = with(density) { rect.height().toDp() },
                                    ),
                            )
                        }
                    }
                }
            }

            dim.textArea?.let { ta ->
                if (!bottomCaption.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(density) { ta.height().toDp() }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = bottomCaption,
                            style = TextStyle(
                                fontSize = captionSp,
                                fontWeight = FontWeight.Medium,
                                color = brandColor.copy(alpha = 0.95f),
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            val lastBottom = dim.cells.maxOfOrNull { it.bottom } ?: dim.headerArea.bottom
            val bottomRemainPx = dim.canvasHeight - lastBottom -
                if (dim.hasBottomText && dim.textArea != null) dim.textArea.height() else 0f
            if (bottomRemainPx > 0.5f) {
                Spacer(Modifier.height(with(density) { bottomRemainPx.toDp() }))
            }
        }
    }
}

/**
 * [CollagePreview]를 부모 영역 안에 빠짐없이 보이도록 등비 축소한다.
 */
@Composable
fun CollagePreviewScaledToFit(
    images: List<Bitmap>,
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    overrideBackground: Color? = null,
    bottomCaption: String? = null,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val maxW = constraints.maxWidth
        val maxH = constraints.maxHeight
        if (maxW == 0) {
            return@SubcomposeLayout layout(0, 0) {}
        }

        val innerConstraints = Constraints(
            maxWidth = maxW,
            maxHeight = Constraints.Infinity,
        )
        val placeable = subcompose("collagePreview") {
            CollagePreview(
                images = images,
                frameType = frameType,
                frameStyle = frameStyle,
                theme = theme,
                overrideBackground = overrideBackground,
                bottomCaption = bottomCaption,
                modifier = Modifier.fillMaxWidth(),
            )
        }[0].measure(innerConstraints)

        val pw = placeable.width
        val ph = placeable.height
        if (pw == 0 || ph == 0) {
            return@SubcomposeLayout layout(maxW.coerceAtLeast(constraints.minWidth), 0) {}
        }

        val scaleW = maxW.toFloat() / pw
        val scaleH = if (maxH != Constraints.Infinity) maxH.toFloat() / ph else Float.POSITIVE_INFINITY
        val scale = min(min(scaleW, scaleH), 1f).coerceAtLeast(0f)

        val scaledW = (pw * scale).roundToInt().coerceAtLeast(1)
        val scaledH = (ph * scale).roundToInt().coerceAtLeast(1)

        val layoutW = when {
            constraints.hasBoundedWidth -> constraints.maxWidth
            else -> scaledW.coerceIn(constraints.minWidth, constraints.maxWidth)
        }
        val layoutH = when {
            constraints.hasBoundedHeight -> constraints.maxHeight
            else -> scaledH.coerceIn(constraints.minHeight, constraints.maxHeight)
        }

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

@Composable
private fun CollagePreviewCell(
    bitmap: Bitmap?,
    cellOverlay: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clip(RectangleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cellOverlay),
        )
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.fillMaxSize(0.4f),
            )
        }
    }
}

private fun brandTextColor(effectiveBackground: Color): Color =
    if (isBlackBackground(effectiveBackground)) {
        Color.White
    } else {
        Color.Black.copy(alpha = 0.9f)
    }

private fun cellPlaceholderOverlay(effectiveBackground: Color): Color =
    if (isBlackBackground(effectiveBackground)) {
        Color.Black.copy(alpha = 0.25f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

private fun isBlackBackground(color: Color): Boolean =
    color == Color.Black || color == Color(0xFF000000)
