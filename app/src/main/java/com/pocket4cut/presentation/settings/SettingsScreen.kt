package com.pocket4cut.presentation.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocket4cut.data.local.SessionRepository
import com.pocket4cut.data.storage.FileImageStorage
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.*
import com.pocket4cut.ui.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onContactFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var galleryCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        galleryCount = withContext(Dispatchers.IO) {
            SessionRepository(context).getAll().size
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppSpacing.Screen.top,
                        start = AppSpacing.Screen.horizontal,
                        end = AppSpacing.Screen.horizontal,
                        bottom = AppSpacing.md,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("설정", style = AppTypography.title1, color = AppColors.Text.primary)
                    Text(
                        "앱을 내 취향대로 꾸며보세요",
                        style = AppTypography.subheadline,
                        color = AppColors.Text.secondary,
                    )
                }
                IconCircleButton(onClick = onBack, variant = IconButtonVariant.SOLID) {
                    Icon(Icons.Default.Close, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppSpacing.Screen.horizontal)
                    .padding(top = AppSpacing.md, bottom = AppSpacing.xxxl),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Component.sectionGap),
            ) {
                ThemeSection(context)
                CameraSection()
                SaveSection()
                DataSection(galleryCount) { showDeleteConfirm = true }
                AboutSection(
                    onPrivacyPolicy = onPrivacyPolicy,
                    onContactFeedback = onContactFeedback,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        ConfirmDialog(
            visible = showDeleteConfirm,
            title = "보관함 전체 삭제",
            message = "저장된 ${galleryCount}개의 추억이 모두 삭제돼요.\n이 작업은 되돌릴 수 없어요.",
            confirmText = "전체 삭제",
            cancelText = "취소",
            isDestructive = true,
            onConfirm = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val repo = SessionRepository(context)
                        val imageStorage = FileImageStorage(context)
                        repo.getAll().forEach { session ->
                            imageStorage.deleteSessionFiles(session.id)
                            repo.delete(session.id)
                        }
                    }
                    galleryCount = 0
                    showDeleteConfirm = false
                    snackbarHostState.showSnackbar(
                        message = "삭제 완료",
                        duration = SnackbarDuration.Short,
                    )
                }
            },
            onCancel = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun ThemeSection(context: android.content.Context) {
    val currentSeason = ThemeManager.currentSeason
    SettingsGroup(title = "테마", icon = Icons.Default.Palette) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Season.entries.forEach { season ->
                val isSelected = currentSeason == season
                val theme = season.theme()
                ScaleOnPress(onClick = { ThemeManager.setTheme(context, season) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected) Modifier.background(
                                    AppColors.Accent.pinkSubtle,
                                    RoundedCornerShape(AppLayout.Radius.md),
                                ) else Modifier
                            )
                            .padding(vertical = AppSpacing.sm, horizontal = AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(theme.background.card)
                                    .border(1.dp, theme.border.medium, CircleShape),
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(theme.accent.pink),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                season.displayName,
                                style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
                                color = AppColors.Text.primary,
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = AppColors.Accent.pink,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraSection() {
    SettingsGroup(title = "카메라", icon = Icons.Default.CameraAlt) {
        Column {
            ToggleRow(
                title = "전면 카메라 기본",
                subtitle = "촬영 시작 시 셀카 모드로",
                icon = Icons.Default.Cameraswitch,
                checked = AppSettings.preferFrontCamera,
                onCheckedChange = { AppSettings.updatePreferFrontCamera(it) },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AppSpacing.md),
                color = AppColors.Border.subtle,
            )
            CountdownSlider()
        }
    }
}

@Composable
private fun CountdownSlider() {
    var sliderValue by remember { mutableFloatStateOf(AppSettings.countdownSeconds.toFloat()) }
    Row(
        modifier = Modifier.padding(vertical = AppSpacing.sm, horizontal = AppSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Icon(
            Icons.Default.Timer,
            null,
            tint = AppColors.Text.secondary,
            modifier = Modifier
                .size(28.dp)
                .padding(top = 2.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        "카운트다운",
                        style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.Text.primary,
                    )
                    Text(
                        "촬영 전 대기 시간 (1~${AppSettings.COUNTDOWN_MAX}초)",
                        style = AppTypography.caption1,
                        color = AppColors.Text.tertiary,
                    )
                }
                Text(
                    "${sliderValue.roundToInt()}초",
                    style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.Accent.pink,
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { AppSettings.updateCountdownSeconds(sliderValue.roundToInt()) },
                valueRange = AppSettings.COUNTDOWN_MIN.toFloat()..AppSettings.COUNTDOWN_MAX.toFloat(),
                steps = AppSettings.COUNTDOWN_MAX - AppSettings.COUNTDOWN_MIN - 1,
                colors = SliderDefaults.colors(
                    thumbColor = AppColors.Accent.pink,
                    activeTrackColor = AppColors.Accent.pink,
                    inactiveTrackColor = AppColors.Border.subtle,
                ),
            )
        }
    }
}

@Composable
private fun SaveSection() {
    SettingsGroup(title = "저장", icon = Icons.Default.Download) {
        Column {
            ToggleRow(
                title = "자동 갤러리 저장",
                subtitle = "완성 후 자동으로 사진첩에 저장",
                icon = Icons.Default.SaveAlt,
                checked = AppSettings.autoSaveToGallery,
                onCheckedChange = { AppSettings.updateAutoSaveToGallery(it) },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AppSpacing.md),
                color = AppColors.Border.subtle,
            )
            ToggleRow(
                title = "날짜 기본 표시",
                subtitle = "편집 시 날짜가 기본으로 켜져요",
                icon = Icons.Default.CalendarToday,
                checked = AppSettings.showDateByDefault,
                onCheckedChange = { AppSettings.updateShowDateByDefault(it) },
            )
        }
    }
}

@Composable
private fun DataSection(galleryCount: Int, onDeleteRequest: () -> Unit) {
    SettingsGroup(title = "데이터 관리", icon = Icons.Default.Storage) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDeleteRequest)
                .padding(vertical = AppSpacing.sm, horizontal = AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        ) {
            Icon(
                Icons.Default.Delete,
                null,
                tint = AppColors.Semantic.error,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "보관함 전체 삭제",
                    style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.Semantic.error,
                )
                Text(
                    "저장된 콜라주 ${galleryCount}개",
                    style = AppTypography.caption1,
                    color = AppColors.Text.tertiary,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = AppColors.Text.tertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun AboutSection(
    onPrivacyPolicy: () -> Unit,
    onContactFeedback: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
            "${info.versionName} ($code)"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
    SettingsGroup(title = "앱 정보", icon = Icons.Default.Info) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.sm, horizontal = AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "버전",
                    style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.Text.primary,
                )
                Text(version, style = AppTypography.callout, color = AppColors.Text.tertiary)
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AppSpacing.md),
                color = AppColors.Border.subtle,
            )
            LinkRow(title = "개인정보 처리방침", icon = Icons.Default.Lock, onClick = onPrivacyPolicy)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AppSpacing.md),
                color = AppColors.Border.subtle,
            )
            LinkRow(title = "문의 / 피드백", icon = Icons.Default.Email, onClick = onContactFeedback)
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            modifier = Modifier.padding(start = AppSpacing.xs),
        ) {
            Icon(icon, null, tint = AppColors.Accent.pink, modifier = Modifier.size(14.dp))
            Text(title, style = AppTypography.headline, color = AppColors.Text.primary)
        }
        val shape = RoundedCornerShape(AppLayout.Radius.lg)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AppColors.Background.card)
                .border(AppLayout.BorderWidth.thin, AppColors.Border.subtle, shape),
        ) {
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.sm, horizontal = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Icon(icon, null, tint = AppColors.Text.secondary, modifier = Modifier.size(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Text.primary,
            )
            Text(subtitle, style = AppTypography.caption1, color = AppColors.Text.tertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.Accent.pink,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = AppColors.Border.light,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun LinkRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AppSpacing.sm, horizontal = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Icon(icon, null, tint = AppColors.Text.secondary, modifier = Modifier.size(28.dp))
        Text(
            title,
            style = AppTypography.callout.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.Text.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Default.ChevronRight, null, tint = AppColors.Text.tertiary, modifier = Modifier.size(16.dp))
    }
}
