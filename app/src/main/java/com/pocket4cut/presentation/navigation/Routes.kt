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
    const val DETAIL_EDIT = "detailEdit"
    const val RESULT = "result"

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

    /**
     * Navigation 문자열 인자에 파일 경로를 넣을 때는 URL 인코딩보다 Base64(URL_SAFE)가 안전하다.
     * (`+`, `/`, `%` 등이 Nav 파싱·디코딩 단계에서 깨지거나 잘리는 이슈 방지)
     */
    fun encodePath(path: String): String =
        android.util.Base64.encodeToString(
            path.toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP,
        )

    fun decodePath(encoded: String): String {
        if (encoded.isBlank()) return ""
        return runCatching {
            val bytes = android.util.Base64.decode(
                encoded,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP,
            )
            String(bytes, Charsets.UTF_8)
        }.recoverCatching {
            // 예전 URLEncoder 버전 호환
            java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name())
        }.getOrElse { encoded }
    }
}

