package com.pocket4cut.frame

import android.graphics.RectF
import kotlin.math.roundToInt

/**
 * [CollageRenderer]와 동일한 캔버스·셀 기하. 미리보기(WYSIWYG)와 export가 같은 수식을 쓴다.
 */
data class CollageLayoutDimensions(
    val canvasWidth: Float,
    val canvasHeight: Float,
    val scale: Float,
    val cells: List<RectF>,
    /** 브랜드 타이틀 영역 (렌더러 drawBrandTitle과 동일) */
    val headerArea: RectF,
    val textArea: RectF?,
    val hasBottomText: Boolean,
)

internal const val COLLAGE_CLASSIC_WIDTH_PX = 1650f

/** 브랜드 "Pocket 4Cut" 기준 크기 — [CollageRenderer]·[CollagePreview]에서 scale 곱해 사용 */
internal const val BRAND_TITLE_TEXT_PT = 22f

object CollageLayoutMath {

    private const val CLASSIC_W = COLLAGE_CLASSIC_WIDTH_PX
    private const val CLASSIC_H = 4920f
    private const val HEADER_PT = 90f
    private const val TEXT_BAND_PT = 80f

    fun compute(
        frameStyle: FrameStyle,
        theme: FrameTheme,
        text: String?,
        dateString: String?,
        outputWidthPx: Float,
    ): CollageLayoutDimensions {
        val hasText = !text.isNullOrBlank() || !dateString.isNullOrBlank()
        return if (frameStyle.id == FrameLayoutId.FOUR_VERTICAL) {
            fourVertical(frameStyle, theme, hasText)
        } else {
            generic(frameStyle, theme, hasText, outputWidthPx)
        }
    }

    /** 미리보기: 부모 너비에 맞춰 스케일할 때 사용 (캔버스 비율 유지). */
    fun computeForPreview(
        frameStyle: FrameStyle,
        theme: FrameTheme,
        bottomCaption: String?,
        containerWidthPx: Float,
    ): CollageLayoutDimensions {
        val hasText = !bottomCaption.isNullOrBlank()
        val full = compute(
            frameStyle,
            theme,
            if (hasText) " " else null,
            if (hasText) " " else null,
            if (frameStyle.id == FrameLayoutId.FOUR_VERTICAL) CLASSIC_W else containerWidthPx.coerceAtLeast(1f),
        )
        val scalePreview = containerWidthPx / full.canvasWidth
        return CollageLayoutDimensions(
            canvasWidth = full.canvasWidth * scalePreview,
            canvasHeight = full.canvasHeight * scalePreview,
            scale = full.scale * scalePreview,
            cells = full.cells.map { r ->
                RectF(
                    r.left * scalePreview,
                    r.top * scalePreview,
                    r.right * scalePreview,
                    r.bottom * scalePreview,
                )
            },
            headerArea = RectF(
                full.headerArea.left * scalePreview,
                full.headerArea.top * scalePreview,
                full.headerArea.right * scalePreview,
                full.headerArea.bottom * scalePreview,
            ),
            textArea = full.textArea?.let { a ->
                RectF(
                    a.left * scalePreview,
                    a.top * scalePreview,
                    a.right * scalePreview,
                    a.bottom * scalePreview,
                )
            },
            hasBottomText = hasText,
        )
    }

    private fun fourVertical(
        style: FrameStyle,
        theme: FrameTheme,
        hasText: Boolean,
    ): CollageLayoutDimensions {
        val w = CLASSIC_W
        val h = CLASSIC_H
        val scaleRef = w / 390f
        val rows = style.rows
        val headerHeight = HEADER_PT * scaleRef
        val textAreaHeight = if (hasText) TEXT_BAND_PT * scaleRef else 0f
        val padding = theme.outerPadding * scaleRef
        val spacing = theme.cellSpacing * scaleRef
        val contentWidth = w - padding * 2f
        val rowGaps = spacing * (rows - 1)
        val chrome = headerHeight + padding * 2f + textAreaHeight + rowGaps
        val cellHeight = (h - chrome) / rows

        val cells = buildList {
            for (r in 0 until rows) {
                val x = padding
                val y = padding + headerHeight + r * (cellHeight + spacing)
                add(RectF(x, y, x + contentWidth, y + cellHeight))
            }
        }

        return CollageLayoutDimensions(
            canvasWidth = w,
            canvasHeight = h,
            scale = scaleRef,
            cells = cells,
            headerArea = RectF(0f, 0f, w, padding + headerHeight),
            textArea = if (hasText) RectF(0f, h - textAreaHeight - padding, w, h) else null,
            hasBottomText = hasText,
        )
    }

    private fun generic(
        style: FrameStyle,
        theme: FrameTheme,
        hasText: Boolean,
        outputWidth: Float,
    ): CollageLayoutDimensions {
        val w = outputWidth
        val scale = w / 390f
        val rows = style.rows
        val cols = style.columns
        val headerHeight = HEADER_PT * scale
        val textAreaHeight = if (hasText) TEXT_BAND_PT * scale else 0f
        val (padPt, gapPt) = effectiveOuterPaddingCellSpacing(style, theme)
        val padding = padPt * scale
        val spacing = gapPt * scale
        val contentWidth = w - padding * 2f
        val cellAspect = style.cellAspectWidthOverHeight
        val cellWidth = (contentWidth - spacing * (cols - 1)) / cols
        val cellHeight = cellWidth / cellAspect
        val contentHeight = cellHeight * rows + spacing * (rows - 1).coerceAtLeast(0)
        val canvasHeight = headerHeight + contentHeight + padding * 2f + textAreaHeight

        val cells = buildList {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x = padding + c * (cellWidth + spacing)
                    val y = padding + headerHeight + r * (cellHeight + spacing)
                    add(RectF(x, y, x + cellWidth, y + cellHeight))
                }
            }
        }

        return CollageLayoutDimensions(
            canvasWidth = w,
            canvasHeight = canvasHeight,
            scale = scale,
            cells = cells,
            headerArea = RectF(0f, 0f, w, padding + headerHeight),
            textArea = if (hasText) {
                RectF(0f, canvasHeight - textAreaHeight - padding, w, canvasHeight)
            } else {
                null
            },
            hasBottomText = hasText,
        )
    }

    /**
     * 2컷 가로 레이아웃만 여백을 20/10으로 — 세로 2컷·그 외는 [FrameTheme] 값 사용.
     */
    private fun effectiveOuterPaddingCellSpacing(style: FrameStyle, theme: FrameTheme): Pair<Float, Float> =
        if (style.id == FrameLayoutId.TWO_HORIZONTAL) 20f to 10f
        else theme.outerPadding to theme.cellSpacing
}

/** 렌더러에서 사용 — [CollageRenderer]가 내부 Layout 대신 이걸 쓰도록 연결 */
internal fun collageLayoutForRender(
    frameStyle: FrameStyle,
    theme: FrameTheme,
    text: String?,
    dateString: String?,
    outputWidth: Int,
): CollageLayoutDimensions {
    val w = outputWidth.toFloat().coerceAtLeast(1f)
    return if (frameStyle.id == FrameLayoutId.FOUR_VERTICAL) {
        CollageLayoutMath.compute(
            frameStyle,
            theme,
            text,
            dateString,
            COLLAGE_CLASSIC_WIDTH_PX,
        )
    } else {
        CollageLayoutMath.compute(frameStyle, theme, text, dateString, w)
    }
}

