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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocket4cut.ui.designsystem.*

enum class ConfirmDialogVariant {
    Default,
    Destructive,
}

@Composable
fun ConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    variant: ConfirmDialogVariant = ConfirmDialogVariant.Default,
    isDestructive: Boolean = false,
) {
    if (!visible) return
    val effectiveVariant =
        if (isDestructive || variant == ConfirmDialogVariant.Destructive) {
            ConfirmDialogVariant.Destructive
        } else {
            ConfirmDialogVariant.Default
        }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Overlay.heavy)
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.xl)
                    .clip(RoundedCornerShape(AppLayout.Radius.xl))
                    .background(AppColors.Background.tertiary)
                    .border(1.dp, AppColors.Border.light, RoundedCornerShape(AppLayout.Radius.xl))
                    .clickable(enabled = false, onClick = {})
                    .padding(AppSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = title, style = AppTypography.title3, color = AppColors.Text.primary)
                Spacer(Modifier.height(AppSpacing.sm))
                Text(
                    text = message,
                    style = AppTypography.callout,
                    color = AppColors.Text.secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(AppSpacing.xl))
                if (effectiveVariant == ConfirmDialogVariant.Destructive) {
                    DestructiveButton(text = confirmText, onClick = onConfirm, fullWidth = true)
                } else {
                    PrimaryButton(text = confirmText, onClick = onConfirm, fullWidth = true)
                }
                Spacer(Modifier.height(AppSpacing.sm))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppLayout.Height.Button.lg)
                        .clip(RoundedCornerShape(AppLayout.Radius.md))
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = cancelText, style = AppTypography.headline, color = AppColors.Text.secondary)
                }
            }
        }
    }
}
