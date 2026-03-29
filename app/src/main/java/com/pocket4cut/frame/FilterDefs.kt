package com.pocket4cut.frame

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

enum class FilterId(val displayName: String, val iconName: String) {
    ORIGINAL("원본", "photo"),
    SOFT("소프트", "sun.max"),
    FILM("필름", "film"),
    BW("흑백", "circle.lefthalf.filled"),
}

object FilterDefs {
    fun colorFilter(id: FilterId): ColorMatrixColorFilter? {
        val matrix = when (id) {
            FilterId.ORIGINAL -> return null
            FilterId.SOFT -> ColorMatrix().apply {
                setSaturation(0.85f)
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.03f, 0.02f, 0f, 0f, 8f,
                            0.01f, 1.02f, 0f, 0f, 6f,
                            0f, 0f, 0.98f, 0f, 5f,
                            0f, 0f, 0f, 1f, 0f,
                        ),
                    ),
                )
            }
            FilterId.FILM -> ColorMatrix().apply {
                setSaturation(0.7f)
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.08f, 0.06f, -0.02f, 0f, 6f,
                            0.03f, 1.04f, 0f, 0f, 4f,
                            -0.04f, 0f, 0.86f, 0f, -4f,
                            0f, 0f, 0f, 1f, 0f,
                        ),
                    ),
                )
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            0.94f, 0f, 0f, 0f, 10f,
                            0f, 0.94f, 0f, 0f, 10f,
                            0f, 0f, 0.94f, 0f, 10f,
                            0f, 0f, 0f, 1f, 0f,
                        ),
                    ),
                )
            }
            FilterId.BW -> ColorMatrix().apply {
                setSaturation(0f)
            }
        }
        return ColorMatrixColorFilter(matrix)
    }
}
