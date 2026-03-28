package com.pocket4cut.presentation.navigation

import com.pocket4cut.core.util.Constants

object Routes {
    const val HOME = "home"
    const val GALLERY = "gallery"
    const val FRAME_TYPE_SELECT = "frameTypeSelect"
    const val CAPTURE = "capture"
    const val SELECTION = "selection"
    const val FRAME_THEME = "frameTheme"
    const val EDIT = "edit"
    const val RESULT = "result"
    const val RESULT_EDIT = "resultEdit"

    object Args {
        const val FRAME_TYPE = "frameType"
        const val SESSION_ID = "sessionId"
        const val SELECTED_INDEXES = "selectedIndexes"
        const val THEME_ID = "themeId"
        const val RESULT_PATH = "resultPath"
    }
}

enum class FrameType(val id: String, val captureCount: Int, val selectCount: Int) {
    TWO_CUT(id = "2", captureCount = Constants.CAPTURE_COUNT_2_CUT, selectCount = Constants.SELECT_COUNT_2_CUT),
    FOUR_CUT(id = "4", captureCount = Constants.CAPTURE_COUNT_4_CUT, selectCount = Constants.SELECT_COUNT_4_CUT),
    SIX_CUT(id = "6", captureCount = Constants.CAPTURE_COUNT_6_CUT, selectCount = Constants.SELECT_COUNT_6_CUT);

    companion object {
        fun fromId(id: String): FrameType = when (id) {
            TWO_CUT.id -> TWO_CUT
            FOUR_CUT.id -> FOUR_CUT
            SIX_CUT.id -> SIX_CUT
            else -> FOUR_CUT
        }
    }
}

object NavCodec {
    fun encodeIndexes(indexes: List<Int>): String = indexes.joinToString(separator = ",")

    fun decodeIndexes(raw: String): List<Int> =
        raw.split(",")
            .mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() }?.toIntOrNull() }

    fun encodePath(path: String): String = java.net.URLEncoder.encode(path, Charsets.UTF_8.name())
    fun decodePath(encoded: String): String = java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name())
}

