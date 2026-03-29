package com.pocket4cut.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.pocket4cut.ui.designsystem.AppColors

private val Pocket4CutColorScheme = darkColorScheme(
    primary = AppColors.Accent.pink,
    onPrimary = AppColors.Text.primary,
    secondary = AppColors.Accent.pinkLight,
    onSecondary = AppColors.Text.primary,
    tertiary = AppColors.Accent.pinkDark,
    background = AppColors.Background.primary,
    onBackground = AppColors.Text.primary,
    surface = AppColors.Background.secondary,
    onSurface = AppColors.Text.primary,
    surfaceVariant = AppColors.Background.tertiary,
    onSurfaceVariant = AppColors.Text.secondary,
    error = AppColors.Semantic.error,
    onError = AppColors.Text.primary,
    outline = AppColors.Border.light,
    outlineVariant = AppColors.Border.subtle,
)

@Composable
fun Pocket4CutTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = Pocket4CutColorScheme,
        typography = Typography,
        content = content,
    )
}
