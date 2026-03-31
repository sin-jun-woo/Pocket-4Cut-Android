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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.pocket4cut.core.util.AppFontCatalog
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.ui.designsystem.theme.Season
import kotlin.math.min
import kotlin.math.roundToInt

private const val BrandTitle = "Pocket 4Cut"

@Composable
fun CollagePreview(
    images: List<Bitmap>,
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    overrideBackground: Color? = null,
    overrideBackgroundImage: Bitmap? = null,
    customFrameDesign: CustomFrameDesign? = null,
    bottomCaption: String? = null,
    captionTextPart: String? = null,
    captionDatePart: String? = null,
    captionTextSizePt: Float = 16f,
    captionDateSizePt: Float = 16f,
    captionFontName: String? = null,
    captionColorRGB: Long? = null,
    modifier: Modifier = Modifier,
) {
    val seasonHTML = customFrameDesign?.resolvedSeason
    val effectiveBackground = when {
        seasonHTML != null -> {
            val hex = SeasonHTMLFrameStyle.baseHex(seasonHTML)
            Color((0xFF000000 or hex).toInt())
        }
        overrideBackground != null -> overrideBackground
        customFrameDesign != null -> customFrameDesign.resolvedFillColor
        else -> theme.background
    }
    val used = images.take(frameType.selectCount)
    val brandColor = brandTextColor(effectiveBackground)
    val cellOverlay = cellPlaceholderOverlay(effectiveBackground)
    val useSeasonBackdrop = seasonHTML != null
    val shape = if (useSeasonBackdrop) RoundedCornerShape(0.dp)
    else RoundedCornerShape(theme.cornerRadius.dp)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val containerPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val dim = remember(containerPx, frameStyle, theme, bottomCaption) {
            CollageLayoutMath.computeForPreview(frameStyle, theme, bottomCaption, containerPx)
        }

        val brandFontSp = with(density) { (BRAND_TITLE_TEXT_PT * dim.scale).toSp() }
        val captionTextSp: TextUnit = with(density) { (captionTextSizePt * dim.scale).toSp() }
        val captionDateSp: TextUnit = with(density) { (captionDateSizePt * dim.scale).toSp() }
        val captionSpLegacy = with(density) { (captionTextSizePt * dim.scale).toSp() }

        val seasonGradient = if (seasonHTML != null) SeasonHTMLFrameStyle.canvasGradientBrush(seasonHTML) else null

        val borderColor = if (seasonHTML != null) {
            Color((0xFF000000 or SeasonHTMLFrameStyle.outerStrokeHex(seasonHTML)).toInt())
        } else theme.border
        val borderW = if (seasonHTML != null) {
            SeasonHTMLFrameStyle.outerBorderWidthPoints(seasonHTML)
        } else theme.borderWidth

        Column(
            modifier = Modifier
                .width(with(density) { dim.canvasWidth.toDp() })
                .height(with(density) { dim.canvasHeight.toDp() })
                .clip(shape)
                .background(effectiveBackground, shape)
                .then(
                    if (seasonGradient != null) Modifier.drawBehind {
                        drawRect(brush = seasonGradient)
                    } else Modifier
                )
                .border(borderW.dp, borderColor, shape),
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

            val cellCornerRatio = if (useSeasonBackdrop) SeasonHTMLFrameStyle.CELL_CORNER_RATIO else 0f
            val cellStrokeColor = if (seasonHTML != null) {
                Color((0xFF000000 or SeasonHTMLFrameStyle.cellStrokeHex(seasonHTML)).toInt())
            } else Color.Transparent

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
                            val cellCorner = rect.width() * cellCornerRatio
                            val cellShape = if (cellCorner > 0f) RoundedCornerShape(with(density) { cellCorner.toDp() })
                            else RectangleShape
                            CollagePreviewCell(
                                bitmap = used.getOrNull(idx),
                                cellOverlay = cellOverlay,
                                iconTint = brandColor.copy(alpha = 0.35f),
                                cellShape = cellShape,
                                seasonStrokeColor = cellStrokeColor,
                                cellCornerPx = cellCorner,
                                scale = dim.scale,
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
                val useSplit =
                    captionTextPart != null || captionDatePart != null
                val hasSplitContent =
                    !captionTextPart.isNullOrBlank() || !captionDatePart.isNullOrBlank()
                val legacyCaption = bottomCaption?.takeIf { it.isNotBlank() }
                val showCaption = (useSplit && hasSplitContent) || (!useSplit && legacyCaption != null)
                if (showCaption) {
                    val context = LocalContext.current
                    val captionFont = if (captionFontName != null) {
                        AppFontCatalog.fontFamily(context, captionFontName)
                    } else {
                        null
                    }
                    val captionColor = if (captionColorRGB != null) {
                        Color((0xFF000000 or (captionColorRGB and 0xFFFFFF)).toInt())
                    } else {
                        brandColor.copy(alpha = 0.95f)
                    }
                    val baseStyle = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontFamily = captionFont,
                        color = captionColor,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(density) { ta.height().toDp() }),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (useSplit && hasSplitContent) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (!captionTextPart.isNullOrBlank()) {
                                    Text(
                                        text = captionTextPart,
                                        style = baseStyle.copy(fontSize = captionTextSp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (!captionTextPart.isNullOrBlank() && !captionDatePart.isNullOrBlank()) {
                                    Text(
                                        text = " · ",
                                        style = baseStyle.copy(fontSize = captionTextSp),
                                        maxLines = 1,
                                    )
                                }
                                if (!captionDatePart.isNullOrBlank()) {
                                    Text(
                                        text = captionDatePart,
                                        style = baseStyle.copy(fontSize = captionDateSp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = legacyCaption.orEmpty(),
                                style = baseStyle.copy(fontSize = captionSpLegacy),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
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

@Composable
fun CollagePreviewScaledToFit(
    images: List<Bitmap>,
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    overrideBackground: Color? = null,
    overrideBackgroundImage: Bitmap? = null,
    customFrameDesign: CustomFrameDesign? = null,
    bottomCaption: String? = null,
    captionTextPart: String? = null,
    captionDatePart: String? = null,
    captionTextSizePt: Float = 16f,
    captionDateSizePt: Float = 16f,
    captionFontName: String? = null,
    captionColorRGB: Long? = null,
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
                overrideBackgroundImage = overrideBackgroundImage,
                customFrameDesign = customFrameDesign,
                bottomCaption = bottomCaption,
                captionTextPart = captionTextPart,
                captionDatePart = captionDatePart,
                captionTextSizePt = captionTextSizePt,
                captionDateSizePt = captionDateSizePt,
                captionFontName = captionFontName,
                captionColorRGB = captionColorRGB,
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
    cellShape: androidx.compose.ui.graphics.Shape,
    seasonStrokeColor: Color,
    cellCornerPx: Float,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(cellShape)
            .then(
                if (seasonStrokeColor != Color.Transparent && cellCornerPx >= 0f) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = seasonStrokeColor,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellCornerPx, cellCornerPx),
                            style = Stroke(
                                width = 3f * scale,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * scale, 6f * scale), 0f),
                            ),
                        )
                    }
                } else Modifier
            ),
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
    if (isDark(effectiveBackground)) Color.White
    else Color.Black.copy(alpha = 0.9f)

private fun cellPlaceholderOverlay(effectiveBackground: Color): Color =
    if (isDark(effectiveBackground)) Color.Black.copy(alpha = 0.25f)
    else Color.Black.copy(alpha = 0.06f)

private fun isDark(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance <= 0.5f
}
