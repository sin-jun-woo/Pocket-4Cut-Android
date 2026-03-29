package com.pocket4cut.ui.designsystem.theme

import androidx.compose.ui.graphics.Color

val SpringTheme = object : AppThemeData {
    override val season = Season.SPRING
    override val background = ThemeBackgroundColors(Color(0xFFFFF9F5), Color(0xFFFFF5F0), Color(0xFFFFEBE5), Color.White)
    override val text = ThemeTextColors(Color(0xFF2D2D2D), Color(0xFF6B6B6B), Color(0xFFA0A0A0), Color(0xFFFF8FB8), Color.White)
    override val accent = ThemeAccentColors(
        pink = Color(0xFFFFB4D6), coral = Color(0xFFFFCDB8), peach = Color(0xFFFFD4B8),
        yellow = Color(0xFFFFF4C4), mint = Color(0xFFC4F5E1), sky = Color(0xFFC4E5FF),
        lavender = Color(0xFFE5D4FF), rose = Color(0xFFFFD4E5),
        pinkLight = Color(0xFFFFD4E5), pinkDark = Color(0xFFFF8FB3),
    )
    override val gradient = ThemeGradients(
        sunsetColors = listOf(Color(0xFFFFB4D6), Color(0xFFFFCDB8), Color(0xFFFFF4C4)),
        candyColors = listOf(Color(0xFFFFB4D6), Color(0xFFE5D4FF)),
        peachyColors = listOf(Color(0xFFFFD4B8), Color(0xFFFFB4D6)),
        dreamyColors = listOf(Color(0xFFC4E5FF), Color(0xFFE5D4FF)),
        freshColors = listOf(Color(0xFFC4F5E1), Color(0xFFC4E5FF)),
    )
    override val border = ThemeBorderColors(Color(0xFFFFE5DD), Color(0xFFFFD4C4), Color(0xFFFFB4D6), Color(0x33FFB4D6))
    override val shadow = ThemeShadowColors(Color(0x33FFB4D6), Color(0x40FFB4D6), Color(0x66FFB4D6))
    override val semantic = ThemeSemanticColors(Color(0xFFA8E6CF), Color(0xFFFFD4A3), Color(0xFFFFB4B4), Color(0xFFB4D4FF))
    override val overlay = ThemeOverlayColors(Color(0x1AFFFFFF), Color(0x80000000), Color(0xB3000000), Color(0x99FFFFFF))
    override val decorationEmojis = listOf("💕", "⭐", "✨", "🌸")
    override val tipEmoji = "💫"
}

val SummerTheme = object : AppThemeData {
    override val season = Season.SUMMER
    override val background = ThemeBackgroundColors(Color(0xFFE6F7FF), Color(0xFFCCF0FF), Color(0xFFB3E8FF), Color.White)
    override val text = ThemeTextColors(Color(0xFF2D2D2D), Color(0xFF5A5A5A), Color(0xFF8A8A8A), Color(0xFFFF6B9D), Color.White)
    override val accent = ThemeAccentColors(
        pink = Color(0xFFFF6B9D), coral = Color(0xFFFF7E67), peach = Color(0xFFFFB347),
        yellow = Color(0xFFFFD93D), mint = Color(0xFF6BCF9F), sky = Color(0xFF4FC3F7),
        lavender = Color(0xFF9B8CFF), rose = Color(0xFFFF85A1),
        pinkLight = Color(0xFFFF85A1), pinkDark = Color(0xFFE5527F),
    )
    override val gradient = ThemeGradients(
        sunsetColors = listOf(Color(0xFFFFD93D), Color(0xFFFF7E67), Color(0xFFFF6B9D)),
        candyColors = listOf(Color(0xFFFF85A1), Color(0xFF9B8CFF)),
        peachyColors = listOf(Color(0xFFFFB347), Color(0xFFFF7E67)),
        dreamyColors = listOf(Color(0xFF4FC3F7), Color(0xFF9B8CFF)),
        freshColors = listOf(Color(0xFF6BCF9F), Color(0xFF4FC3F7)),
    )
    override val border = ThemeBorderColors(Color(0xFFB3E8FF), Color(0xFF4FC3F7), Color(0xFFFF7E67), Color(0x334FC3F7))
    override val shadow = ThemeShadowColors(Color(0x334FC3F7), Color(0x404FC3F7), Color(0x66FF7E67))
    override val semantic = ThemeSemanticColors(Color(0xFF6BCF9F), Color(0xFFFFB347), Color(0xFFFF6B6B), Color(0xFF4FC3F7))
    override val overlay = ThemeOverlayColors(Color(0x1AFFFFFF), Color(0x80000000), Color(0xB3000000), Color(0x99FFFFFF))
    override val decorationEmojis = listOf("🌊", "☀️", "🐚", "🎐")
    override val tipEmoji = "🌊"
}

val AutumnTheme = object : AppThemeData {
    override val season = Season.AUTUMN
    override val background = ThemeBackgroundColors(Color(0xFFFFF8F0), Color(0xFFFFF3E8), Color(0xFFFFE8D6), Color(0xFFFFFDF9))
    override val text = ThemeTextColors(Color(0xFF3D2F2F), Color(0xFF6B5B4F), Color(0xFF9B8B7E), Color(0xFFD97D54), Color.White)
    override val accent = ThemeAccentColors(
        pink = Color(0xFFE8956B), coral = Color(0xFFD97D54), peach = Color(0xFFF4A76B),
        yellow = Color(0xFFF4D58D), mint = Color(0xFFA8B89B), sky = Color(0xFF9AADBD),
        lavender = Color(0xFFB8A8C4), rose = Color(0xFFD99B87),
        pinkLight = Color(0xFFD99B87), pinkDark = Color(0xFFC06030),
    )
    override val gradient = ThemeGradients(
        sunsetColors = listOf(Color(0xFFE8956B), Color(0xFFF4A76B), Color(0xFFF4D58D)),
        candyColors = listOf(Color(0xFFE8956B), Color(0xFFD97D54)),
        peachyColors = listOf(Color(0xFFF4A76B), Color(0xFFE8956B)),
        dreamyColors = listOf(Color(0xFF9AADBD), Color(0xFFB8A8C4)),
        freshColors = listOf(Color(0xFFA8B89B), Color(0xFF9AADBD)),
    )
    override val border = ThemeBorderColors(Color(0xFFFFE8D6), Color(0xFFF4D5C4), Color(0xFFE8956B), Color(0x33E8956B))
    override val shadow = ThemeShadowColors(Color(0x33D97D54), Color(0x40D97D54), Color(0x66E8956B))
    override val semantic = ThemeSemanticColors(Color(0xFFA8B89B), Color(0xFFF4A76B), Color(0xFFD97D7D), Color(0xFF9AADBD))
    override val overlay = ThemeOverlayColors(Color(0x1AFFFFFF), Color(0x80000000), Color(0xB3000000), Color(0x99FFFFFF))
    override val decorationEmojis = listOf("🍂", "🍁", "🌰", "🦊")
    override val tipEmoji = "🍂"
}

val WinterTheme = object : AppThemeData {
    override val season = Season.WINTER
    override val background = ThemeBackgroundColors(Color(0xFFF0F8FF), Color(0xFFE6F4FF), Color(0xFFD6EFFF), Color.White)
    override val text = ThemeTextColors(Color(0xFF2D3E50), Color(0xFF5A6C7D), Color(0xFF8B9AAA), Color(0xFF5B9BD5), Color.White)
    override val accent = ThemeAccentColors(
        pink = Color(0xFFA8D8EA), coral = Color(0xFFB8E1F5), peach = Color(0xFFC8E8FF),
        yellow = Color(0xFFE0F4FF), mint = Color(0xFFA8E6CF), sky = Color(0xFF89CFF0),
        lavender = Color(0xFFC4D7F2), rose = Color(0xFFB8D8E8),
        pinkLight = Color(0xFFB8D8E8), pinkDark = Color(0xFF6BAACC),
    )
    override val gradient = ThemeGradients(
        sunsetColors = listOf(Color(0xFF89CFF0), Color(0xFFA8D8EA), Color(0xFFC8E8FF)),
        candyColors = listOf(Color(0xFFA8D8EA), Color(0xFFC4D7F2)),
        peachyColors = listOf(Color(0xFFB8E1F5), Color(0xFFA8D8EA)),
        dreamyColors = listOf(Color(0xFF89CFF0), Color(0xFFC4D7F2)),
        freshColors = listOf(Color(0xFFA8E6CF), Color(0xFF89CFF0)),
    )
    override val border = ThemeBorderColors(Color(0xFFD6EFFF), Color(0xFFC8E8FF), Color(0xFFA8D8EA), Color(0x33A8D8EA))
    override val shadow = ThemeShadowColors(Color(0x3389CFF0), Color(0x4089CFF0), Color(0x66A8D8EA))
    override val semantic = ThemeSemanticColors(Color(0xFFA8E6CF), Color(0xFFB8D8E8), Color(0xFFC4A8D8), Color(0xFF89CFF0))
    override val overlay = ThemeOverlayColors(Color(0x1AFFFFFF), Color(0x80000000), Color(0xB3000000), Color(0x99FFFFFF))
    override val decorationEmojis = listOf("❄️", "⛄", "🌨️", "💎")
    override val tipEmoji = "❄️"
}

fun Season.theme(): AppThemeData = when (this) {
    Season.SPRING -> SpringTheme
    Season.SUMMER -> SummerTheme
    Season.AUTUMN -> AutumnTheme
    Season.WINTER -> WinterTheme
}
