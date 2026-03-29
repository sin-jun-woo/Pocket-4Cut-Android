package com.pocket4cut.frame

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

enum class FilterId(val displayName: String) {
    ORIGINAL("원본"),
    BW("흑백"),
    WARM("따뜻함"),
    COOL("차가움"),
    VINTAGE("빈티지"),
    VIVID("선명함"),
    SOFT("부드러움"),
}

object FilterDefs {
    fun colorFilter(id: FilterId): ColorMatrixColorFilter? {
        val matrix = when (id) {
            FilterId.ORIGINAL -> return null
            FilterId.BW -> ColorMatrix().apply {
                setSaturation(0f)
            }
            FilterId.WARM -> ColorMatrix().apply {
                setSaturation(1.2f)
                postConcat(ColorMatrix(floatArrayOf(
                    1.1f, 0.05f, 0f, 0f, 10f,
                    0f, 1.05f, 0.02f, 0f, 5f,
                    0f, 0f, 0.95f, 0f, -5f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
            FilterId.COOL -> ColorMatrix().apply {
                setSaturation(0.8f)
                postConcat(ColorMatrix(floatArrayOf(
                    0.9f, 0f, 0.05f, 0f, -5f,
                    0f, 0.95f, 0.05f, 0f, 0f,
                    0.05f, 0.05f, 1.1f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
            FilterId.VINTAGE -> ColorMatrix().apply {
                setSaturation(0.7f)
                postConcat(ColorMatrix(floatArrayOf(
                    1.1f, 0.1f, 0f, 0f, 8f,
                    0f, 1.0f, 0.05f, 0f, 5f,
                    -0.05f, 0f, 0.9f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
            FilterId.VIVID -> ColorMatrix().apply {
                setSaturation(1.5f)
                postConcat(ColorMatrix(floatArrayOf(
                    1.1f, 0f, 0f, 0f, 0f,
                    0f, 1.1f, 0f, 0f, 0f,
                    0f, 0f, 1.1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
            FilterId.SOFT -> ColorMatrix().apply {
                setSaturation(0.85f)
                postConcat(ColorMatrix(floatArrayOf(
                    1.05f, 0f, 0f, 0f, 12f,
                    0f, 1.05f, 0f, 0f, 12f,
                    0f, 0f, 1.05f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            }
        }
        return ColorMatrixColorFilter(matrix)
    }
}
