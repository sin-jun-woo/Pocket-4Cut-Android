package com.pocket4cut.ui.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class ThemeBackgroundColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val card: Color,
)

data class ThemeTextColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
    val inverse: Color,
)

data class ThemeAccentColors(
    val pink: Color,
    val coral: Color,
    val peach: Color,
    val yellow: Color,
    val mint: Color,
    val sky: Color,
    val lavender: Color,
    val rose: Color,
    val pinkLight: Color,
    val pinkDark: Color,
) {
    val pinkSubtle: Color get() = pink.copy(alpha = 0.10f)
}

data class ThemeGradients(
    val sunsetColors: List<Color>,
    val candyColors: List<Color>,
    val peachyColors: List<Color>,
    val dreamyColors: List<Color>,
    val freshColors: List<Color>,
) {
    val sunset: Brush get() = Brush.linearGradient(sunsetColors, start = Offset.Zero, end = Offset.Infinite)
    val candy: Brush get() = Brush.linearGradient(candyColors, start = Offset.Zero, end = Offset.Infinite)
    val peachy: Brush get() = Brush.linearGradient(peachyColors, start = Offset.Zero, end = Offset.Infinite)
    val dreamy: Brush get() = Brush.linearGradient(dreamyColors, start = Offset.Zero, end = Offset.Infinite)
    val fresh: Brush get() = Brush.linearGradient(freshColors, start = Offset.Zero, end = Offset.Infinite)
}

data class ThemeBorderColors(
    val light: Color,
    val medium: Color,
    val accent: Color,
    val subtle: Color,
)

data class ThemeShadowColors(
    val color: Color,
    val colorMd: Color,
    val glow: Color,
)

data class ThemeSemanticColors(
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
)

data class ThemeOverlayColors(
    val light: Color,
    val medium: Color,
    val heavy: Color,
    val white: Color,
)

interface AppThemeData {
    val season: Season
    val background: ThemeBackgroundColors
    val text: ThemeTextColors
    val accent: ThemeAccentColors
    val gradient: ThemeGradients
    val border: ThemeBorderColors
    val shadow: ThemeShadowColors
    val semantic: ThemeSemanticColors
    val overlay: ThemeOverlayColors
    val decorationEmojis: List<String>
    val tipEmoji: String
}
