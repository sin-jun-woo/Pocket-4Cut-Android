package com.pocket4cut.frame

import androidx.compose.ui.graphics.Color
import com.pocket4cut.ui.designsystem.theme.Season
import java.util.UUID

data class NormPoint(
    val x: Float = 0.5f,
    val y: Float = 0.5f,
)

data class CustomFrameDecoration(
    val id: String = UUID.randomUUID().toString(),
    var position: NormPoint = NormPoint(0.5f, 0.5f),
    var scale: Float = 1f,
    var rotationRadians: Float = 0f,
    var fontName: String? = null,
    var kind: Kind,
) {
    sealed class Kind {
        data class Text(
            val content: String,
            val textColorARGB: Long,
            val fontScale: Float,
        ) : Kind()

        data class Emoji(val content: String) : Kind()

        data class Sticker(
            val assetId: String,
            val colorRGB: Long,
        ) : Kind()
    }
}

data class CustomFrameDesign(
    val fillColorId: String,
    val fillHex: Long? = null,
    val sourceSeason: String? = null,
    val decorations: List<CustomFrameDecoration> = emptyList(),
) {
    val fillFrameColor: FrameColor?
        get() = FrameColors.all.firstOrNull { it.id == fillColorId }

    val resolvedFillColor: Color
        get() {
            val h = fillHex
            if (h != null) return Color(0xFF000000 or h)
            return fillFrameColor?.color ?: Color.White
        }

    val usesSeasonHTMLBackdrop: Boolean
        get() = sourceSeason != null

    val resolvedSeason: Season?
        get() = when (sourceSeason) {
            "spring" -> Season.SPRING
            "summer" -> Season.SUMMER
            "autumn" -> Season.AUTUMN
            "winter" -> Season.WINTER
            else -> null
        }
}
