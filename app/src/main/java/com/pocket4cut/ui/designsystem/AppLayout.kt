package com.pocket4cut.ui.designsystem

import androidx.compose.ui.unit.dp

object AppLayout {
    object Radius {
        val none = 0.dp
        val xs = 8.dp
        val sm = 12.dp
        val md = 16.dp
        val lg = 20.dp
        val xl = 24.dp
        val xxl = 32.dp
        val full = 9999.dp
    }

    object Height {
        object Button {
            val sm = 36.dp
            val md = 44.dp
            val lg = 56.dp
        }
        object Input {
            val default = 44.dp
            val large = 56.dp
        }
        val touchTargetMin = 44.dp
        object IconButton {
            val sm = 36.dp
            val md = 44.dp
            val lg = 56.dp
        }
    }
}
