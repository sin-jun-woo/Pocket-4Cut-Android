package com.pocket4cut.frame

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

sealed class FrameBackgroundSelection {
    data class CatalogTheme(val theme: FrameTheme) : FrameBackgroundSelection()
    data class SolidColor(val baseTheme: FrameTheme, val color: FrameColor) : FrameBackgroundSelection()
    data class CustomImage(val baseTheme: FrameTheme, val image: Bitmap) : FrameBackgroundSelection()
    data class CustomDecorated(val baseTheme: FrameTheme, val design: CustomFrameDesign) : FrameBackgroundSelection()

    val structuralTheme: FrameTheme
        get() = when (this) {
            is CatalogTheme -> theme
            is SolidColor -> baseTheme
            is CustomImage -> baseTheme
            is CustomDecorated -> baseTheme
        }

    val catalogThemeIfApplicable: FrameTheme?
        get() = (this as? CatalogTheme)?.theme

    val colorOverride: Color?
        get() = when (this) {
            is SolidColor -> color.color
            is CustomDecorated -> design.resolvedFillColor
            else -> null
        }

    val customBackgroundImage: Bitmap?
        get() = (this as? CustomImage)?.image

    val customFrameDesign: CustomFrameDesign?
        get() = (this as? CustomDecorated)?.design

    val allowsColorEditInEditor: Boolean
        get() = when (this) {
            is SolidColor -> true
            is CustomDecorated -> design.sourceSeason == null
            else -> false
        }
}
