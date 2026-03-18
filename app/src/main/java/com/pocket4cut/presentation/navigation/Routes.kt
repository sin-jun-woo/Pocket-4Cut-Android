package com.pocket4cut.presentation.navigation

object Routes {
    const val HOME = "home"
    const val FRAME_TYPE_SELECT = "frameTypeSelect"
    const val CAPTURE = "capture"
    const val SELECTION = "selection"

    object Args {
        const val FRAME_TYPE = "frameType"
        const val SESSION_ID = "sessionId"
    }
}

enum class FrameType(val id: String, val captureCount: Int, val selectCount: Int) {
    FOUR_CUT(id = "4", captureCount = 8, selectCount = 4),
    SIX_CUT(id = "6", captureCount = 10, selectCount = 6);

    companion object {
        fun fromId(id: String): FrameType = when (id) {
            FOUR_CUT.id -> FOUR_CUT
            SIX_CUT.id -> SIX_CUT
            else -> FOUR_CUT
        }
    }
}

