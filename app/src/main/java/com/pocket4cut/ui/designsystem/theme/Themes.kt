package com.pocket4cut.ui.designsystem.theme

import androidx.compose.ui.graphics.Color

/*
 * The app shell uses a shared paper/ink base in every season. Seasonal colour
 * remains available for frames and small accents, so changing a theme never
 * turns the whole product into a different pastel gradient.
 */

private val Paper = Color(0xFFF3F0E8)
private val PaperAlt = Color(0xFFEFEAE0)
private val Surface = Color(0xFFFBFAF6)
private val PaperLine = Color(0xFFD0CBC1)
private val Ink = Color(0xFF1B1B19)
private val Muted = Color(0xFF6E6A63)
private val Quiet = Color(0xFF706B63)

private fun shellTheme(
    themeSeason: Season,
    accentColor: Color,
    accentDark: Color,
    secondaryAccents: List<Color>,
    decorations: List<String>,
    tipMarker: String,
): AppThemeData {
    require(secondaryAccents.size == 7)
    val coral = secondaryAccents[0]
    val peach = secondaryAccents[1]
    val yellow = secondaryAccents[2]
    val mint = secondaryAccents[3]
    val sky = secondaryAccents[4]
    val lavender = secondaryAccents[5]
    val rose = secondaryAccents[6]
    return object : AppThemeData {
        override val season = themeSeason
        override val background = ThemeBackgroundColors(Paper, PaperAlt, Color(0xFFE7E1D7), Surface)
        override val text = ThemeTextColors(Ink, Muted, Quiet, accentColor, Surface)
        override val accent = ThemeAccentColors(
            pink = accentColor,
            coral = coral,
            peach = peach,
            yellow = yellow,
            mint = mint,
            sky = sky,
            lavender = lavender,
            rose = rose,
            pinkLight = rose,
            pinkDark = accentDark,
        )
        // Kept for frame APIs; all stops are intentionally close to one hue.
        override val gradient = ThemeGradients(
            sunsetColors = listOf(accentColor, peach),
            candyColors = listOf(accentColor, rose),
            peachyColors = listOf(peach, coral),
            dreamyColors = listOf(sky, lavender),
            freshColors = listOf(mint, sky),
        )
        override val border = ThemeBorderColors(PaperLine, Color(0xFFADA59A), accentColor, Color(0x26ADA59A))
        override val shadow = ThemeShadowColors(Color(0x261B1B19), Color(0x331B1B19), Color(0x291B1B19))
        override val semantic = ThemeSemanticColors(Color(0xFF3E7653), Color(0xFF996D2B), Color(0xFFB3372B), Color(0xFF3E6983))
        override val overlay = ThemeOverlayColors(Color(0x1AFFFFFF), Color(0x80000000), Color(0xB3000000), Color(0x99FFFFFF))
        override val decorationEmojis = decorations
        override val tipEmoji = tipMarker
    }
}

val SpringTheme = shellTheme(
    themeSeason = Season.SPRING,
    accentColor = Color(0xFFC83D2D),
    accentDark = Color(0xFF9E2C22),
    secondaryAccents = listOf(
        Color(0xFFB94A3B), Color(0xFFD8A276), Color(0xFFD7B04A),
        Color(0xFF6D8D7A), Color(0xFF537A9A), Color(0xFF777091), Color(0xFFC97968),
    ),
    decorations = listOf("봄", "01", "04", "꽃"),
    tipMarker = "※",
)

val SummerTheme = shellTheme(
    themeSeason = Season.SUMMER,
    accentColor = Color(0xFF006D77),
    accentDark = Color(0xFF004B52),
    secondaryAccents = listOf(
        Color(0xFF0B7A75), Color(0xFFE09850), Color(0xFFC4A33A),
        Color(0xFF4F856A), Color(0xFF3D7190), Color(0xFF6C6792), Color(0xFF6DA3A0),
    ),
    decorations = listOf("여름", "02", "04", "바다"),
    tipMarker = "※",
)

val AutumnTheme = shellTheme(
    themeSeason = Season.AUTUMN,
    accentColor = Color(0xFFA64B2B),
    accentDark = Color(0xFF7A321D),
    secondaryAccents = listOf(
        Color(0xFF8E4027), Color(0xFFD1844C), Color(0xFFBF963F),
        Color(0xFF71806B), Color(0xFF5B7684), Color(0xFF75667B), Color(0xFFBB735B),
    ),
    decorations = listOf("가을", "03", "04", "나뭇잎"),
    tipMarker = "※",
)

val WinterTheme = shellTheme(
    themeSeason = Season.WINTER,
    accentColor = Color(0xFF345D82),
    accentDark = Color(0xFF203F5C),
    secondaryAccents = listOf(
        Color(0xFF446E8B), Color(0xFFA8B2BA), Color(0xFFB4A866),
        Color(0xFF5F857B), Color(0xFF4E769C), Color(0xFF6F7391), Color(0xFF7A98A9),
    ),
    decorations = listOf("겨울", "04", "02", "눈"),
    tipMarker = "※",
)

fun Season.theme(): AppThemeData = when (this) {
    Season.SPRING -> SpringTheme
    Season.SUMMER -> SummerTheme
    Season.AUTUMN -> AutumnTheme
    Season.WINTER -> WinterTheme
}
