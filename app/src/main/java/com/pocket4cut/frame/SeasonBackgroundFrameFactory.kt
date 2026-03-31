package com.pocket4cut.frame

import com.pocket4cut.ui.designsystem.theme.Season

object SeasonBackgroundFrameFactory {
    private const val PLACEHOLDER_FILL_ID = "ivory"

    fun design(season: Season): CustomFrameDesign = when (season) {
        Season.SPRING -> CustomFrameDesign(
            fillColorId = PLACEHOLDER_FILL_ID,
            fillHex = SeasonHTMLFrameStyle.baseHex(Season.SPRING),
            sourceSeason = "spring",
            decorations = emptyList(),
        )
        Season.SUMMER -> CustomFrameDesign(
            fillColorId = PLACEHOLDER_FILL_ID,
            fillHex = SeasonHTMLFrameStyle.baseHex(Season.SUMMER),
            sourceSeason = "summer",
            decorations = emptyList(),
        )
        Season.AUTUMN -> CustomFrameDesign(
            fillColorId = PLACEHOLDER_FILL_ID,
            fillHex = SeasonHTMLFrameStyle.baseHex(Season.AUTUMN),
            sourceSeason = "autumn",
            decorations = emptyList(),
        )
        Season.WINTER -> CustomFrameDesign(
            fillColorId = PLACEHOLDER_FILL_ID,
            fillHex = SeasonHTMLFrameStyle.baseHex(Season.WINTER),
            sourceSeason = "winter",
            decorations = emptyList(),
        )
    }
}
