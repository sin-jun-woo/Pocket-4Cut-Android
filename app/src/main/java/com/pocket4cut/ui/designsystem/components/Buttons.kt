package com.pocket4cut.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography

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

/** A small compatibility wrapper for older screens; the press state is now a native ripple. */
@Composable
fun ScaleOnPress(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
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
    val shape = RoundedCornerShape(AppLayout.Radius.sm)
    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height())
            .clip(shape)
            .background(if (enabled) AppColors.Accent.pink else AppColors.Text.tertiary)
            .clickable(enabled = enabled, onClick = onClick),
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
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    size: AppButtonSize = AppButtonSize.LG,
    icon: @Composable (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(AppLayout.Radius.sm)
    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height())
            .clip(shape)
            .background(AppColors.Background.card)
            .border(AppLayout.BorderWidth.thin, AppColors.Border.medium, shape)
            .clickable(enabled = enabled, onClick = onClick),
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
    val shape = RoundedCornerShape(AppLayout.Radius.sm)
    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height())
            .clip(shape)
            .background(if (enabled) AppColors.Semantic.error else AppColors.Text.tertiary)
            .clickable(enabled = enabled, onClick = onClick),
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

/** Icon-only actions are intentionally square in the print-booth visual language. */
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
    val resolvedSize = diameter ?: presetSize.toLayoutDp()
    val shape = RoundedCornerShape(AppLayout.Radius.sm)
    val bg = when (variant) {
        IconButtonVariant.SOLID -> AppColors.Background.tertiary
        IconButtonVariant.DEFAULT -> AppColors.Background.card
    }
    Box(
        modifier = modifier
            .size(resolvedSize)
            .clip(shape)
            .background(bg)
            .border(AppLayout.BorderWidth.thin, AppColors.Border.light, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
