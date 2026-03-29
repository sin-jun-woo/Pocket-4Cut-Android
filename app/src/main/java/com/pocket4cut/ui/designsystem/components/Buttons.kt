package com.pocket4cut.ui.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocket4cut.ui.designsystem.*

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
    icon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) AppAnimation.Scale.pressed else 1f, label = "scale")

    val bg = if (enabled) AppColors.Accent.pink else AppColors.Text.tertiary

    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(AppLayout.Height.Button.lg)
            .scale(scale)
            .clip(RoundedCornerShape(AppLayout.Radius.md))
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(AppSpacing.xs))
        }
        Text(
            text = text,
            style = AppTypography.headline,
            color = AppColors.Text.primary,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
    icon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) AppAnimation.Scale.pressed else 1f, label = "scale")

    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(AppLayout.Height.Button.lg)
            .scale(scale)
            .clip(RoundedCornerShape(AppLayout.Radius.md))
            .background(AppColors.Overlay.white)
            .border(1.dp, AppColors.Border.medium, RoundedCornerShape(AppLayout.Radius.md))
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(AppSpacing.xs))
        }
        Text(
            text = text,
            style = AppTypography.headline,
            color = if (enabled) AppColors.Text.primary else AppColors.Text.tertiary,
        )
    }
}

@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) AppAnimation.Scale.pressed else 1f, label = "scale")

    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(AppLayout.Height.Button.lg)
            .scale(scale)
            .clip(RoundedCornerShape(AppLayout.Radius.md))
            .background(if (enabled) AppColors.Semantic.error else AppColors.Text.tertiary)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = AppTypography.headline, color = AppColors.Text.primary)
    }
}

enum class IconButtonVariant { DEFAULT, SOLID }

@Composable
fun IconCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = AppLayout.Height.IconButton.md,
    variant: IconButtonVariant = IconButtonVariant.DEFAULT,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) AppAnimation.Scale.pressed else 1f, label = "scale")

    val bg = when (variant) {
        IconButtonVariant.SOLID -> AppColors.Background.tertiary
        IconButtonVariant.DEFAULT -> AppColors.Overlay.white
    }
    val borderColor = when (variant) {
        IconButtonVariant.SOLID -> Color.Transparent
        IconButtonVariant.DEFAULT -> AppColors.Border.light
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(bg)
            .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, CircleShape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
