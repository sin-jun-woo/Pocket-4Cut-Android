package com.pocket4cut.ui.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocket4cut.ui.designsystem.*

enum class AppButtonSize {
    SM,
    MD,
    LG,
}

private fun AppButtonSize.height(): Dp = when (this) {
    AppButtonSize.SM -> AppLayout.Height.Button.sm
    AppButtonSize.MD -> AppLayout.Height.Button.md
    AppButtonSize.LG -> AppLayout.Height.Button.lg
}

enum class IconCircleButtonSize {
    SM,
    MD,
    LG,
}

fun IconCircleButtonSize.toLayoutDp(): Dp = when (this) {
    IconCircleButtonSize.SM -> AppLayout.Height.IconButton.sm
    IconCircleButtonSize.MD -> AppLayout.Height.IconButton.md
    IconCircleButtonSize.LG -> AppLayout.Height.IconButton.lg
}

fun IconCircleButtonSize.toIconDp(): Dp = when (this) {
    IconCircleButtonSize.SM -> 14.dp
    IconCircleButtonSize.MD -> 18.dp
    IconCircleButtonSize.LG -> 22.dp
}

enum class IconButtonVariant { DEFAULT, SOLID }

@Composable
fun ScaleOnPress(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AppAnimation.Scale.pressed else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "scalePress",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
    ) {
        content()
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    size: AppButtonSize = AppButtonSize.LG,
    icon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AppAnimation.Scale.pressed else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "scale",
    )

    val bg = if (enabled) AppColors.Accent.pink else AppColors.Text.tertiary
    val shape = RoundedCornerShape(AppLayout.Radius.md)

    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 16.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = AppColors.Shadow.glow,
                        spotColor = AppColors.Shadow.glow,
                    )
                } else {
                    Modifier
                },
            )
            .height(size.height())
            .scale(scale)
            .clip(shape)
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
            color = Color.White,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    size: AppButtonSize = AppButtonSize.LG,
    icon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AppAnimation.Scale.pressed else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "scale",
    )

    val shape = RoundedCornerShape(AppLayout.Radius.md)

    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height())
            .scale(scale)
            .clip(shape)
            .background(AppColors.Background.card)
            .border(AppLayout.BorderWidth.thin, AppColors.Border.medium, shape)
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
    fullWidth: Boolean = true,
    size: AppButtonSize = AppButtonSize.LG,
    icon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AppAnimation.Scale.pressed else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "scale",
    )

    val shape = RoundedCornerShape(AppLayout.Radius.md)

    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height())
            .scale(scale)
            .clip(shape)
            .background(if (enabled) AppColors.Semantic.error else AppColors.Text.tertiary)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(AppSpacing.xs))
        }
        Text(text = text, style = AppTypography.headline, color = Color.White)
    }
}

@Composable
fun IconCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    presetSize: IconCircleButtonSize = IconCircleButtonSize.MD,
    diameter: Dp? = null,
    variant: IconButtonVariant = IconButtonVariant.DEFAULT,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AppAnimation.Scale.pressed else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "scale",
    )

    val resolvedSize = diameter ?: presetSize.toLayoutDp()

    val bg = when (variant) {
        IconButtonVariant.SOLID -> AppColors.Background.tertiary
        IconButtonVariant.DEFAULT -> AppColors.Background.card
    }

    Box(
        modifier = modifier
            .size(resolvedSize)
            .scale(scale)
            .clip(CircleShape)
            .background(bg)
            .border(AppLayout.BorderWidth.thin, AppColors.Border.light, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
