package com.pocket4cut.presentation.navigation

import com.pocket4cut.core.util.Constants

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

