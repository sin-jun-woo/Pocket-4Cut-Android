package com.pocket4cut.ui.designsystem.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pocket4cut.ui.designsystem.AppColors

private val ToggleWidth = 52.dp
private val ToggleHeight = 32.dp
private val ThumbSize = 28.dp
private val ThumbPadding = 2.dp

@Composable
fun PinkToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val thumbTargetX = if (checked) {
        ToggleWidth - ThumbPadding - ThumbSize
    } else {
        ThumbPadding
    }
    val thumbX by animateDpAsState(
        targetValue = thumbTargetX,
        animationSpec = tween(durationMillis = 160),
        label = "pinkToggleThumb",
    )
    val trackColor = if (checked) AppColors.Accent.pink else AppColors.Border.medium

    Box(
        modifier = modifier
            .size(ToggleWidth, 48.dp)
            .semantics { contentDescription = label }
            .toggleable(
                value = checked,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ToggleWidth, ToggleHeight)
                .clip(RoundedCornerShape(ToggleHeight / 2))
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbX, y = ThumbPadding)
                    .size(ThumbSize)
                    .clip(CircleShape)
                    .background(AppColors.Text.inverse),
            )
        }
    }
}
