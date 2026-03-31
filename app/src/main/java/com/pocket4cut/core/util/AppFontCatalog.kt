package com.pocket4cut.core.util

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

data class FontOption(
    val id: String,
    val displayName: String,
    val fileName: String?,
) {
    var typeface: Typeface? = null
        internal set
    var fontFamily: FontFamily? = null
        internal set
}

object AppFontCatalog {
    private val bundledFiles = listOf(
        "기본" to null,
        "배민 한나체 Pro" to "BMHANNAPro.ttf",
        "배민 주아체" to "BMJUA_ttf.ttf",
        "배민 꾸불림체" to "BMKkubulimTTF.ttf",
        "배민 연성체" to "BMYEONSUNG_ttf.ttf",
        "카페24 동동" to "Cafe24DongdongLight.ttf",
        "카페24 모야모야" to "Cafe24Moyamoya-Face-v1.0.ttf",
        "카페24 빛나는별" to "Cafe24Shiningstar-v2.0.ttf",
        "잘난체" to "Jalnan2TTF.ttf",
        "나눔 반짝반짝 별" to "NanumBanJjagBanJjagByeor.ttf",
        "나눔 둥근인연" to "NanumDungGeunInYeon.ttf",
        "나눔 꽃내음" to "NanumGgocNaeEum.ttf",
        "나눔 곰신체" to "NanumGomSinCe.ttf",
        "나눔 느릿느릿체" to "NanumNeuRisNeuRisCe.ttf",
    )

    private var _options: List<FontOption>? = null

    fun options(context: Context): List<FontOption> {
        _options?.let { return it }
        val resolved = mutableListOf<FontOption>()
        resolved.add(FontOption(id = "system", displayName = "기본", fileName = null).also {
            it.typeface = Typeface.DEFAULT
            it.fontFamily = FontFamily.Default
        })
        for ((displayName, fileName) in bundledFiles) {
            if (fileName == null) continue
            val tf = runCatching {
                Typeface.createFromAsset(context.assets, "fonts/$fileName")
            }.getOrNull() ?: continue
            val option = FontOption(
                id = fileName.substringBefore('.'),
                displayName = displayName,
                fileName = fileName,
            ).also {
                it.typeface = tf
                it.fontFamily = FontFamily(
                    Font("fonts/$fileName", context.assets, FontWeight.Normal),
                )
            }
            resolved.add(option)
        }
        _options = resolved
        return resolved
    }

    fun typeface(context: Context, fontName: String?): Typeface {
        if (fontName.isNullOrEmpty()) return Typeface.DEFAULT
        val opts = options(context)
        return opts.firstOrNull { it.id == fontName || it.fileName?.substringBefore('.') == fontName }
            ?.typeface ?: Typeface.DEFAULT
    }

    fun fontFamily(context: Context, fontName: String?): FontFamily {
        if (fontName.isNullOrEmpty()) return FontFamily.Default
        val opts = options(context)
        return opts.firstOrNull { it.id == fontName || it.fileName?.substringBefore('.') == fontName }
            ?.fontFamily ?: FontFamily.Default
    }
}
