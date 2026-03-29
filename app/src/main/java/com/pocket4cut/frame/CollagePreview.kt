package com.pocket4cut.frame

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocket4cut.presentation.navigation.FrameType

private const val BrandTitle = "Pocket 4Cut"

@Composable
fun CollagePreview(
    images: List<Bitmap>,
    frameType: FrameType,
    frameStyle: FrameStyle,
    theme: FrameTheme,
    overrideBackground: Color? = null,
    modifier: Modifier = Modifier,
) {
    val effectiveBackground = overrideBackground ?: theme.background
    val used = images.take(frameType.selectCount)
    val brandColor = brandTextColor(effectiveBackground)
    val cellOverlay = cellPlaceholderOverlay(effectiveBackground)
    val shape = RoundedCornerShape(theme.cornerRadius.dp)
    val spacing = theme.cellSpacing.dp
    val outer = theme.outerPadding.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(effectiveBackground, shape)
            .border(theme.borderWidth.dp, theme.border, shape),
    ) {
        Text(
            text = BrandTitle,
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 24.sp,
                color = brandColor,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = outer, start = outer, end = outer),
        )
        CollagePreviewGrid(
            images = used,
            frameStyle = frameStyle,
            cellOverlay = cellOverlay,
            horizontalPadding = outer,
            bottomPadding = outer,
            spacing = spacing,
            brandColor = brandColor,
        )
    }
}

@Composable
private fun CollagePreviewGrid(
    images: List<Bitmap>,
    frameStyle: FrameStyle,
    cellOverlay: Color,
    horizontalPadding: Dp,
    bottomPadding: Dp,
    spacing: Dp,
    brandColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(frameStyle.rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                repeat(frameStyle.columns) { col ->
                    val index = row * frameStyle.columns + col
                    CollagePreviewCell(
                        bitmap = images.getOrNull(index),
                        cellOverlay = cellOverlay,
                        iconTint = brandColor.copy(alpha = 0.35f),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(frameStyle.cellAspectWidthOverHeight),
                    )
                }
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
