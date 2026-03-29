package com.pocket4cut.frame

import androidx.compose.ui.graphics.Color
import com.pocket4cut.presentation.navigation.FrameType

data class FrameTheme(
    val id: String,
    val name: String,
    val frameType: FrameType,
    val background: Color,
    val border: Color,
    val borderWidth: Float,
    val cornerRadius: Float,
    val outerPadding: Float,
    val cellSpacing: Float,
)

object FrameCatalog {
    fun themes(frameType: FrameType): List<FrameTheme> = when (frameType) {
        FrameType.TWO_CUT -> listOf(
            FrameTheme(
                id = "two_clean_white",
                name = "클린 화이트",
                frameType = FrameType.TWO_CUT,
                background = Color(0xFFFFFFFF),
                border = Color(0x26000000),
                borderWidth = 2f,
                cornerRadius = 0f,
                outerPadding = 14f,
                cellSpacing = 10f,
            ),
            FrameTheme(
                id = "two_film_black",
                name = "필름 블랙",
                frameType = FrameType.TWO_CUT,
                background = Color(0xFF000000),
                border = Color(0x1FFFFFFF),
                borderWidth = 2f,
                cornerRadius = 0f,
                outerPadding = 14f,
                cellSpacing = 10f,
            ),
        )
        FrameType.FOUR_CUT -> listOf(
            FrameTheme(
                id = "four_clean_white",
                name = "클린 화이트",
                frameType = FrameType.FOUR_CUT,
                background = Color(0xFFFFFFFF),
                border = Color(0x26000000),
                borderWidth = 2f,
                cornerRadius = 0f,
                outerPadding = 14f,
                cellSpacing = 10f,
            ),
            FrameTheme(
                id = "four_film_black",
                name = "필름 블랙",
                frameType = FrameType.FOUR_CUT,
                background = Color(0xFF000000),
                border = Color(0x1FFFFFFF),
                borderWidth = 2f,
                cornerRadius = 0f,
                outerPadding = 14f,
                cellSpacing = 10f,
            ),
            FrameTheme(
                id = "four_pastel_mint",
                name = "파스텔 민트",
                frameType = FrameType.FOUR_CUT,
                background = Color(0xFFD4F2ED),
                border = Color(0x59338C80),
                borderWidth = 2f,
                cornerRadius = 0f,
                outerPadding = 16f,
                cellSpacing = 12f,
            ),
        )
        FrameType.SIX_CUT -> listOf(
            FrameTheme(
                id = "six_clean_white",
                name = "클린 화이트",
                frameType = FrameType.SIX_CUT,
                background = Color(0xFFFFFFFF),
                border = Color(0x26000000),
                borderWidth = 2f,
                cornerRadius = 0f,
                outerPadding = 12f,
                cellSpacing = 8f,
            ),
            FrameTheme(
                id = "six_film_black",
                name = "필름 블랙",
                frameType = FrameType.SIX_CUT,
                background = Color(0xFF000000),
                border = Color(0x1FFFFFFF),
                borderWidth = 2f,
                cornerRadius = 0f,
                outerPadding = 12f,
                cellSpacing = 8f,
            ),
        )
    }
}
