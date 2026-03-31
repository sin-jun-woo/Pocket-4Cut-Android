package com.pocket4cut.frame

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

enum class StickerPalette(
    val assetId: String,
    val displayName: String,
    val icon: ImageVector,
) {
    HEART("heart", "하트", Icons.Filled.Favorite),
    STAR("star", "별", Icons.Filled.Star),
    SPARKLES("sparkles", "반짝이", Icons.Filled.AutoAwesome),
    CROWN("crown", "왕관", Icons.Filled.Diamond),
    LEAF("leaf", "잎사귀", Icons.Filled.Spa),
    FLAME("flame", "불꽃", Icons.Filled.LocalFireDepartment),
    BOLT("bolt", "번개", Icons.Filled.Bolt),
    CLOUD("cloud", "구름", Icons.Filled.Cloud),
    MOON("moon", "달", Icons.Filled.NightsStay),
    SNOWFLAKE("snowflake", "눈꽃", Icons.Filled.Air),
    GIFT("gift", "선물", Icons.Filled.Redeem),
    BALLOON("balloon2", "풍선", Icons.Filled.FilterVintage),
    PAWPRINT("pawprint", "발자국", Icons.Filled.Pets),
    FACE_SMILING("faceSmiling", "웃는 얼굴", Icons.Filled.SentimentSatisfied),
    HANDS_SPARKLES("handsSparkles", "반짝 손", Icons.Filled.AutoAwesome),
    DIAMOND("diamond", "다이아", Icons.Filled.Diamond),
    DROP("drop", "물방울", Icons.Filled.WaterDrop),
    MUSIC_NOTE("musicNote", "음표", Icons.Filled.MusicNote),
    CAMERA("camera", "카메라", Icons.Filled.CameraAlt),
    CHECKMARK_SEAL("checkmarkSeal", "체크", Icons.Filled.CheckCircle),
    SUN_MAX("sunMax", "태양", Icons.Filled.WbSunny),
    RAINBOW("rainbow", "무지개", Icons.Filled.ColorLens),
    LADYBUG("ladybug", "무당벌레", Icons.Filled.EmojiNature),
    ANT("ant", "개미", Icons.Filled.EmojiNature),
    TORTOISE("tortoise", "거북이", Icons.Filled.Pets);

    companion object {
        fun fromAssetId(id: String): StickerPalette? = entries.firstOrNull { it.assetId == id }
    }
}
