package com.pocket4cut.frame

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class FrameColor(
    val id: String,
    val name: String,
    val color: Color,
    val gradientBrush: Brush? = null,
) {
    val isLight: Boolean get() = id != "black"
}

object FrameColors {
    val all: List<FrameColor> = listOf(
        // 기본
        FrameColor("white", "화이트", Color(0xFFFFFFFF)),
        FrameColor("black", "블랙", Color(0xFF000000)),
        FrameColor("ivory", "아이보리", Color(0xFFFFFEF0)),
        // 핑크 계열
        FrameColor("blush", "블러쉬", Color(0xFFFFE5E5)),
        FrameColor("rose", "로즈", Color(0xFFFFD6E0)),
        FrameColor("pink", "핑크", Color(0xFFFFC7D5)),
        FrameColor("babypink", "베이비핑크", Color(0xFFFFB3C6)),
        FrameColor("coral", "코랄", Color(0xFFFFB3A7)),
        FrameColor("peach", "피치", Color(0xFFFFD4C2)),
        FrameColor("salmon", "연어", Color(0xFFFFC5B8)),
        // 퍼플/라벤더 계열
        FrameColor("lavender", "라벤더", Color(0xFFE8D5FF)),
        FrameColor("lilac", "라일락", Color(0xFFDCC5F0)),
        FrameColor("periwinkle", "페리윙클", Color(0xFFD4C5F9)),
        FrameColor("mauve", "모브", Color(0xFFE5D0E3)),
        FrameColor("orchid", "오키드", Color(0xFFF3D5F5)),
        FrameColor("wisteria", "위스테리아", Color(0xFFD5BADB)),
        // 블루 계열
        FrameColor("skyblue", "스카이블루", Color(0xFFC7E6FF)),
        FrameColor("babyblue", "베이비블루", Color(0xFFB8E2F2)),
        FrameColor("powder", "파우더블루", Color(0xFFB0D4E8)),
        FrameColor("ice", "아이스블루", Color(0xFFD6F0FF)),
        FrameColor("periwinkleblue", "청보라", Color(0xFFC5D3E8)),
        FrameColor("aqua", "아쿠아", Color(0xFFC2E7E8)),
        // 민트/그린 계열
        FrameColor("mint", "민트", Color(0xFFC7F0DB)),
        FrameColor("seafoam", "시폼", Color(0xFFC7F0E8)),
        FrameColor("sage", "세이지", Color(0xFFD4E8D4)),
        FrameColor("pistachio", "피스타치오", Color(0xFFDFF2D8)),
        FrameColor("matcha", "말차", Color(0xFFD5EDD5)),
        FrameColor("celadon", "청자", Color(0xFFD0E8D0)),
        // 옐로우 계열
        FrameColor("lemon", "레몬", Color(0xFFFFF9C2)),
        FrameColor("butter", "버터", Color(0xFFFFF5D6)),
        FrameColor("vanilla", "바닐라", Color(0xFFFFF8DC)),
        FrameColor("cream", "크림", Color(0xFFFFF8E7)),
        FrameColor("banana", "바나나", Color(0xFFFFF4C2)),
        // 베이지/브라운 계열
        FrameColor("sand", "샌드", Color(0xFFF5E6D3)),
        FrameColor("beige", "베이지", Color(0xFFF5EBD7)),
        FrameColor("mocha", "모카", Color(0xFFE8D5C4)),
        FrameColor("latte", "라떼", Color(0xFFF0E5D8)),
        FrameColor("biscuit", "비스킷", Color(0xFFF2E0C7)),
        // 그레이 계열
        FrameColor("silver", "실버", Color(0xFFE8E8E8)),
        FrameColor("pearl", "펄", Color(0xFFF0F0F0)),
        FrameColor("ash", "애쉬", Color(0xFFE0E0E0)),
        FrameColor("dove", "비둘기", Color(0xFFD5D5D5)),
        // 특별 색상 (gradient)
        FrameColor("hologram", "홀로그램", Color(0xFFF0E5FF),
            Brush.linearGradient(listOf(Color(0xFFFFE5E5), Color(0xFFE5F0FF), Color(0xFFF0E5FF)))),
        FrameColor("sunset", "석양", Color(0xFFFFE5D9),
            Brush.linearGradient(listOf(Color(0xFFFFE5D9), Color(0xFFFFD4E5)))),
        FrameColor("aurora", "오로라", Color(0xFFD4F0FF),
            Brush.linearGradient(listOf(Color(0xFFD4F0FF), Color(0xFFE5D4FF), Color(0xFFFFD4E5)))),
    )

    fun byId(id: String): FrameColor = all.firstOrNull { it.id == id } ?: all.first()
}
