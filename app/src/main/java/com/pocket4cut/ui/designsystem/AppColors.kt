package com.pocket4cut.ui.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pocket4cut.ui.designsystem.theme.ThemeManager

object AppColors {
    private val t get() = ThemeManager.currentTheme

    object Background {
        val primary: Color get() = t.background.primary
        val secondary: Color get() = t.background.secondary
        val tertiary: Color get() = t.background.tertiary
        val card: Color get() = t.background.card
    }

    object Text {
        val primary: Color get() = t.text.primary
        val secondary: Color get() = t.text.secondary
        val tertiary: Color get() = t.text.tertiary
        val accent: Color get() = t.text.accent
        val inverse: Color get() = t.text.inverse
    }

    object Accent {
        val pink: Color get() = t.accent.pink
        val coral: Color get() = t.accent.coral
        val peach: Color get() = t.accent.peach
        val yellow: Color get() = t.accent.yellow
        val mint: Color get() = t.accent.mint
        val sky: Color get() = t.accent.sky
        val lavender: Color get() = t.accent.lavender
        val rose: Color get() = t.accent.rose
        val pinkLight: Color get() = t.accent.pinkLight
        val pinkDark: Color get() = t.accent.pinkDark
        val pinkSubtle: Color get() = t.accent.pinkSubtle
    }

    object Gradient {
        val sunset: Brush get() = t.gradient.sunset
        val candy: Brush get() = t.gradient.candy
        val peachy: Brush get() = t.gradient.peachy
        val dreamy: Brush get() = t.gradient.dreamy
        val fresh: Brush get() = t.gradient.fresh
    }

    object Semantic {
        val success: Color get() = t.semantic.success
        val warning: Color get() = t.semantic.warning
        val error: Color get() = t.semantic.error
        val info: Color get() = t.semantic.info
    }

    object Border {
        val subtle: Color get() = t.border.subtle
        val light: Color get() = t.border.light
        val medium: Color get() = t.border.medium
        val accent: Color get() = t.border.accent
        val focus: Color get() = t.accent.pink
    }

    object Shadow {
        val color: Color get() = t.shadow.color
        val colorMd: Color get() = t.shadow.colorMd
        val glow: Color get() = t.shadow.glow
    }

    object Overlay {
        val light: Color get() = t.overlay.light
        val medium: Color get() = t.overlay.medium
        val heavy: Color get() = t.overlay.heavy
        val white: Color get() = t.overlay.white
    }
}
