package com.pocket4cut.ui.designsystem

import androidx.compose.ui.unit.dp

object AppLayout {
    object Radius {
        val none = 0.dp
        val xs = 2.dp
        val sm = 4.dp
        val md = 6.dp
        val lg = 8.dp
        val xl = 12.dp
        val xxl = 16.dp
        val full = 9999.dp
    }

    object BorderWidth {
        val none = 0.dp
        val thin = 1.dp
        val medium = 1.dp
        val thick = 1.dp
    }

    object Height {
        object Button {
            val sm = 40.dp
            val md = 48.dp
            val lg = 52.dp
        }
        object Input {
            val default = 48.dp
            val large = 60.dp
        }
        val touchTargetMin = 48.dp
        object IconButton {
            val sm = 40.dp
            val md = 48.dp
            val lg = 60.dp
        }
    }
}
