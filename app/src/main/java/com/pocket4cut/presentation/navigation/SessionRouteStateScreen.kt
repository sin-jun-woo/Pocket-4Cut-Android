package com.pocket4cut.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import com.pocket4cut.ui.designsystem.components.SecondaryButton

@Composable
internal fun SessionRouteLoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = AppColors.Accent.pink)
        Spacer(Modifier.height(AppSpacing.md))
        Text(
            text = "작업을 불러오는 중입니다.",
            style = AppTypography.callout,
            color = AppColors.Text.secondary,
        )
    }
}

@Composable
internal fun SessionRecoveryRequiredScreen(
    onOpenGallery: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background.primary)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AppSpacing.Screen.horizontal,
                vertical = AppSpacing.xl,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "이 작업은 복구가 필요합니다.",
            style = AppTypography.title2,
            color = AppColors.Text.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpacing.sm))
        Text(
            text = "저장된 사진과 기존 결과는 바꾸지 않았습니다. 작업 보관함에서 상태를 확인해 주세요.",
            style = AppTypography.body,
            color = AppColors.Text.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpacing.xl))
        PrimaryButton(text = "작업 보관함 열기", onClick = onOpenGallery)
        Spacer(Modifier.height(AppSpacing.sm))
        SecondaryButton(text = "뒤로", onClick = onBack)
    }
}

@Composable
internal fun SessionRouteErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onOpenGallery: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background.primary)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AppSpacing.Screen.horizontal,
                vertical = AppSpacing.xl,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "작업을 불러오지 못했습니다.",
            style = AppTypography.title2,
            color = AppColors.Text.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpacing.sm))
        Text(
            text = message,
            style = AppTypography.body,
            color = AppColors.Text.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppSpacing.xl))
        PrimaryButton(text = "다시 시도", onClick = onRetry)
        Spacer(Modifier.height(AppSpacing.sm))
        SecondaryButton(text = "작업 보관함 열기", onClick = onOpenGallery)
        Spacer(Modifier.height(AppSpacing.sm))
        SecondaryButton(text = "뒤로", onClick = onBack)
    }
}
