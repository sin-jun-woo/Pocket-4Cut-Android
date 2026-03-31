package com.pocket4cut.presentation.navigation

import com.pocket4cut.core.util.Constants

object Routes {
    const val LAUNCH = "launch"
    const val HOME = "home"
    const val GALLERY = "gallery"
    const val FRAME_TYPE_SELECT = "frameTypeSelect"
    const val CAPTURE = "capture"
    const val SELECTION = "selection"
    const val LAYOUT_SELECTION = "layoutSelection"
    const val EDIT = "edit"
    const val DETAIL_EDIT = "detailEdit"
    const val RESULT = "result"
    const val FRAME_THEME_SELECT = "frameThemeSelect"
    const val FRAME_FLOW = "frameFlow"
    const val COLOR_FRAME_PICK = "colorFramePick"
    const val SEASON_FRAME_PICK = "seasonFramePick"
    const val CUSTOM_FRAME_EDITOR = "customFrameEditor"
    const val SETTINGS = "settings"
    const val PRIVACY_POLICY = "privacyPolicy"
    const val CONTACT_FEEDBACK = "contactFeedback"

    object Args {
        const val FRAME_TYPE = "frameType"
        const val SESSION_ID = "sessionId"
        const val SELECTED_INDEXES = "selectedIndexes"
        const val LAYOUT_ID = "layoutId"
        const val RESULT_PATH = "resultPath"
        const val THEME_ID = "themeId"
    }
}

enum class FrameType(
    val id: String,
    val captureCount: Int,
    val selectCount: Int,
    val displayName: String,
    val description: String,
    val subtitle: String,
) {
    TWO_CUT(
        id = "2", captureCount = Constants.CAPTURE_COUNT_2_CUT, selectCount = Constants.SELECT_COUNT_2_CUT,
        displayName = "2컷", description = "2컷 프레임", subtitle = "${Constants.CAPTURE_COUNT_2_CUT}장 촬영 → ${Constants.SELECT_COUNT_2_CUT}장 선택",
    ),
    FOUR_CUT(
        id = "4", captureCount = Constants.CAPTURE_COUNT_4_CUT, selectCount = Constants.SELECT_COUNT_4_CUT,
        displayName = "4컷", description = "4컷 프레임", subtitle = "${Constants.CAPTURE_COUNT_4_CUT}장 촬영 → ${Constants.SELECT_COUNT_4_CUT}장 선택",
    ),
    SIX_CUT(
        id = "6", captureCount = Constants.CAPTURE_COUNT_6_CUT, selectCount = Constants.SELECT_COUNT_6_CUT,
        displayName = "6컷", description = "6컷 프레임", subtitle = "${Constants.CAPTURE_COUNT_6_CUT}장 촬영 → ${Constants.SELECT_COUNT_6_CUT}장 선택",
    );

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
        raw.split(",").mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() }?.toIntOrNull() }

    fun encodePath(path: String): String =
        android.util.Base64.encodeToString(
            path.toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP,
        )

    fun decodePath(encoded: String): String {
        if (encoded.isBlank()) return ""
        return runCatching {
            val bytes = android.util.Base64.decode(encoded, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            String(bytes, Charsets.UTF_8)
        }.recoverCatching {
            java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name())
        }.getOrElse { encoded }
    }
}
