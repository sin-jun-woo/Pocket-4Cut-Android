package com.pocket4cut.ui.designsystem

import androidx.compose.ui.unit.dp

object AppLayout {
    object Radius {
        val none = 0.dp
        val xs = 12.dp
        val sm = 16.dp
        val md = 20.dp
        val lg = 24.dp
        val xl = 28.dp
        val xxl = 36.dp
        val full = 9999.dp
    }

    object BorderWidth {
        val none = 0.dp
        val thin = 2.dp
        val medium = 3.dp
        val thick = 4.dp
    }

    object Height {
        object Button {
            val sm = 40.dp
            val md = 48.dp
            val lg = 60.dp
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
