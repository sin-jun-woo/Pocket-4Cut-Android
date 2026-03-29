package com.pocket4cut.frame

enum class LayoutType { VERTICAL, GRID, HORIZONTAL }

enum class FrameLayoutId(val slots: Int) {
    TWO_VERTICAL(2),
    TWO_HORIZONTAL(2),
    FOUR_VERTICAL(4),
    FOUR_GRID(4),
    FOUR_HORIZONTAL(4),
    SIX_GRID_2X3(6),
    SIX_GRID_3X2(6),
    SIX_COLLAGE(6),
}

data class FrameStyle(
    val id: FrameLayoutId,
    val name: String,
    val description: String,
    val layout: LayoutType,
    val slots: Int,
    val padding: Int,
    val gap: Int,
    val gridColumns: Int = 1,
    val gridRows: Int = 1,
)

object FrameLayouts {
    val all: List<FrameStyle> = listOf(
        FrameStyle(FrameLayoutId.TWO_VERTICAL, "세로 2컷", "클래식한 세로 배치", LayoutType.VERTICAL, 2, 24, 16),
        FrameStyle(FrameLayoutId.TWO_HORIZONTAL, "가로 2컷", "옆으로 나란히", LayoutType.HORIZONTAL, 2, 24, 16),
        FrameStyle(FrameLayoutId.FOUR_VERTICAL, "클래식 4컷", "정통 인생네컷 스타일", LayoutType.VERTICAL, 4, 24, 16),
        FrameStyle(FrameLayoutId.FOUR_GRID, "그리드 4컷", "2x2 그리드 배치", LayoutType.GRID, 4, 20, 12, gridColumns = 2, gridRows = 2),
        FrameStyle(FrameLayoutId.FOUR_HORIZONTAL, "가로 4컷", "영화 필름 스타일", LayoutType.HORIZONTAL, 4, 20, 10),
        FrameStyle(FrameLayoutId.SIX_GRID_2X3, "세로 그리드 6컷", "2열 3행 배치", LayoutType.GRID, 6, 20, 10, gridColumns = 2, gridRows = 3),
        FrameStyle(FrameLayoutId.SIX_GRID_3X2, "가로 그리드 6컷", "3열 2행 배치", LayoutType.GRID, 6, 20, 10, gridColumns = 3, gridRows = 2),
        FrameStyle(FrameLayoutId.SIX_COLLAGE, "콜라주 6컷", "자유로운 배치", LayoutType.GRID, 6, 16, 8, gridColumns = 3, gridRows = 2),
    )

    fun bySlots(slots: Int): List<FrameStyle> = all.filter { it.slots == slots }

    fun byId(id: FrameLayoutId): FrameStyle = all.first { it.id == id }

    fun defaultForSlots(slots: Int): FrameStyle = bySlots(slots).first()
}
