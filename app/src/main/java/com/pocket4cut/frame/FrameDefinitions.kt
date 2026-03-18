package com.pocket4cut.frame

import androidx.compose.ui.graphics.Color
import com.pocket4cut.presentation.navigation.FrameType

data class FrameTheme(
    val id: String,
    val name: String,
    val supportedTypes: Set<FrameType>,
    val background: Color,
    val border: Color,
    val accent: Color,
)

object FrameDefinitions {
    private val themes = listOf(
        FrameTheme(
            id = "classic_white",
            name = "Classic White",
            supportedTypes = setOf(FrameType.TWO_CUT, FrameType.FOUR_CUT, FrameType.SIX_CUT),
            background = Color(0xFFFFFFFF),
            border = Color(0xFF111111),
            accent = Color(0xFF4B7BEC),
        ),
        FrameTheme(
            id = "classic_black",
            name = "Classic Black",
            supportedTypes = setOf(FrameType.TWO_CUT, FrameType.FOUR_CUT, FrameType.SIX_CUT),
            background = Color(0xFF0F0F10),
            border = Color(0xFFEAEAEA),
            accent = Color(0xFFFFC312),
        ),
        // 4컷 전용 1개 더
        FrameTheme(
            id = "soft_pink",
            name = "Soft Pink",
            supportedTypes = setOf(FrameType.FOUR_CUT),
            background = Color(0xFFFFF1F6),
            border = Color(0xFF111111),
            accent = Color(0xFFFF5D8F),
        ),
        // 6컷 전용 1개 더
        FrameTheme(
            id = "mint",
            name = "Mint",
            supportedTypes = setOf(FrameType.SIX_CUT),
            background = Color(0xFFEFFFF7),
            border = Color(0xFF0B3D2E),
            accent = Color(0xFF00B894),
        ),
    )

    fun themesFor(frameType: FrameType): List<FrameTheme> = themes.filter { frameType in it.supportedTypes }
    fun byId(id: String): FrameTheme? = themes.firstOrNull { it.id == id }
}

