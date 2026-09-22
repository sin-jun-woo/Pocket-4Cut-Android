package com.pocket4cut.ui.designsystem.components

import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription

/** Gives the custom drawn sliders the same non-touch controls as a native slider. */
internal fun Modifier.accessibleRange(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
): Modifier {
    fun update(requested: Float): Boolean {
        if (!requested.isFinite()) return false
        val next = requested.coerceIn(range)
        if (next == value) return false
        onValueChange(next)
        return true
    }
    return this
        .semantics {
            contentDescription = label
            stateDescription = valueText
            progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(range), range)
            setProgress { update(it) }
        }
        .onKeyEvent {
            if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
            val step = (range.endInclusive - range.start) / 20f
            when (it.key) {
                Key.DirectionRight, Key.DirectionUp -> { update(value + step); true }
                Key.DirectionLeft, Key.DirectionDown -> { update(value - step); true }
                Key.MoveHome -> { update(range.start); true }
                Key.MoveEnd -> { update(range.endInclusive); true }
                else -> false
            }
        }
        .focusable()
}
