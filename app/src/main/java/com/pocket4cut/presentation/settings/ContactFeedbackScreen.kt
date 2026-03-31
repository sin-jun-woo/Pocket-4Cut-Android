package com.pocket4cut.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocket4cut.ui.designsystem.AppColors
import com.pocket4cut.ui.designsystem.AppLayout
import com.pocket4cut.ui.designsystem.AppSpacing
import com.pocket4cut.ui.designsystem.AppTypography
import com.pocket4cut.ui.designsystem.components.AppToast
import com.pocket4cut.ui.designsystem.components.AppToastType
import com.pocket4cut.ui.designsystem.components.IconButtonVariant
import com.pocket4cut.ui.designsystem.components.IconCircleButton
import com.pocket4cut.ui.designsystem.components.PrimaryButton
import com.pocket4cut.ui.designsystem.components.SecondaryButton
import kotlinx.coroutines.delay

private const val SUPPORT_EMAIL = "sus3456@naver.com"

@Composable
fun ContactFeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showCopiedToast by remember { mutableStateOf(false) }

    LaunchedEffect(showCopiedToast) {
        if (showCopiedToast) {
            delay(2200)
            showCopiedToast = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showCopiedToast) 88.dp else AppSpacing.md),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppSpacing.xxxl,
                        start = AppSpacing.Screen.horizontal,
                        end = AppSpacing.Screen.horizontal,
                        bottom = AppSpacing.md,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircleButton(onClick = onBack, variant = IconButtonVariant.SOLID) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = AppColors.Text.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "문의/피드백",
                    style = AppTypography.title2,
                    color = AppColors.Text.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.width(AppLayout.Height.IconButton.md))
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = AppColors.Border.subtle,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(top = AppSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = SUPPORT_EMAIL,
                    style = AppTypography.title1,
                    color = AppColors.Accent.pink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "버그 제보, 기능 제안, 사용 중 불편한 점이 있으면 위 주소로 메일 보내줘. 최대한 빨리 확인할게.",
                    style = AppTypography.callout,
                    color = AppColors.Text.secondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(AppSpacing.md))
                PrimaryButton(
                    text = "주소 복사",
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("email", SUPPORT_EMAIL))
                        showCopiedToast = true
                    },
                )
                SecondaryButton(
                    text = "메일 앱에서 열기",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$SUPPORT_EMAIL")
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    },
                )
            }
        }
        if (showCopiedToast) {
            AppToast(
                message = "복사되었습니다",
                type = AppToastType.Success,
                onDismiss = { showCopiedToast = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(bottom = AppSpacing.xl),
            )
        }
    }
}
