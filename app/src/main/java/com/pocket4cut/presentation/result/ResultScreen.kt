package com.pocket4cut.presentation.result

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocket4cut.core.util.FileUris
import com.pocket4cut.ui.designsystem.*
import com.pocket4cut.ui.designsystem.components.*
import java.io.File

@Composable
fun ResultScreen(
    resultPath: String,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onGallery: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val file = File(resultPath)
    val uri = FileUris.contentUriForFile(context, file)
    var isSaving by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background.primary)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AppColors.Accent.pink.copy(alpha = 0.06f), Color.Transparent),
                        radius = size.minDimension * 0.7f,
                    ),
                    center = center.copy(y = size.height * 0.3f),
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 220.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.xxxl, start = AppSpacing.Screen.horizontal, end = AppSpacing.Screen.horizontal, bottom = AppSpacing.md),
            ) {
                IconCircleButton(onClick = onHome, variant = IconButtonVariant.SOLID) {
                    Icon(Icons.Default.Home, null, tint = AppColors.Text.secondary, modifier = Modifier.size(20.dp))
                }
            }

            // Title
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = AppSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = AppColors.Accent.pink, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(AppSpacing.xs))
                    Text("완성!", style = AppTypography.largeTitle, color = AppColors.Text.primary)
                    Spacer(Modifier.width(AppSpacing.xs))
                    Icon(Icons.Default.Star, null, tint = AppColors.Accent.pink, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(AppSpacing.sm))
                Text(
                    "소중한 순간이 담긴 사진이 완성되었어요",
                    style = AppTypography.callout,
                    color = AppColors.Text.tertiary,
                    textAlign = TextAlign.Center,
                )
            }

            // Result image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.Screen.horizontal),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(uri).build(),
                    contentDescription = null,
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .aspectRatio(3f / 4f),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        // Bottom actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.Background.primary)
                .padding(horizontal = AppSpacing.Screen.horizontal)
                .padding(bottom = AppSpacing.Layout.ctaBottomSpace, top = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            PrimaryButton(
                text = if (isSaving) "저장 중..." else if (isSaved) "저장 완료" else "갤러리에 저장",
                onClick = {
                    if (isSaving || isSaved) return@PrimaryButton
                    isSaving = true
                    runCatching {
                        val resolver = context.contentResolver
                        val name = file.nameWithoutExtension.ifBlank { "Pocket4Cut_${System.currentTimeMillis()}" }
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            if (Build.VERSION.SDK_INT >= 29) {
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pocket4Cut")
                                put(MediaStore.Images.Media.IS_PENDING, 1)
                            }
                        }
                        val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        val outUri = resolver.insert(collection, values) ?: error("insert failed")
                        resolver.openOutputStream(outUri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                        if (Build.VERSION.SDK_INT >= 29) { values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0); resolver.update(outUri, values, null, null) }
                    }
                    isSaving = false
                    isSaved = true
                },
                enabled = !isSaving,
                fullWidth = true,
                icon = {
                    if (isSaved) Icon(Icons.Default.Check, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                    else if (!isSaving) Icon(Icons.Default.ArrowDownward, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp))
                },
            )

            SecondaryButton(
                text = "공유하기",
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "공유"))
                },
                fullWidth = true,
                icon = { Icon(Icons.Default.Share, null, tint = AppColors.Text.primary, modifier = Modifier.size(20.dp)) },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppLayout.Height.Button.lg)
                    .clickable(onClick = onHome),
                contentAlignment = Alignment.Center,
            ) {
                Text("홈으로 돌아가기", style = AppTypography.callout, color = AppColors.Text.tertiary)
            }
        }
    }
}
