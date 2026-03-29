package com.pocket4cut.ui.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object AppAnimation {
    object Duration {
        const val instant = 100
        const val fast = 200
        const val normal = 300
        const val slow = 500
        const val verySlow = 800
    }

    object Scale {
        const val pressed = 0.96f
        const val hover = 1.02f
        const val tap = 0.92f
    }

    fun <T> defaultSpring() = spring<T>(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMedium,
    )

    fun <T> gentleSpring() = spring<T>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> bouncySpring() = spring<T>(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessMedium,
    )
}
