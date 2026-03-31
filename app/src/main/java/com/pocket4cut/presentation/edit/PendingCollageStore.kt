package com.pocket4cut.presentation.edit

import android.content.Context
import com.pocket4cut.frame.CustomFrameDecoration
import com.pocket4cut.frame.CustomFrameDesign
import com.pocket4cut.frame.FilterId
import com.pocket4cut.frame.NormPoint
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PendingCollageParams(
    val filterId: FilterId,
    val frameColorId: String,
    val themeId: String = "",
    val text: String,
    val showDate: Boolean,
    val order: List<Int>,
    val textFontSize: Float = 16f,
    val dateFontSize: Float = 16f,
    val captionFontName: String? = null,
    val captionColorRGB: Long? = null,
    val frameBackgroundType: String = "solid",
    val seasonId: String? = null,
    val customDesignJson: String? = null,
)

object PendingCollageStore {
    private fun file(ctx: Context, sessionId: String): File =
        File(ctx.filesDir, "pending_collage_$sessionId.json")

    private fun frameSelFile(ctx: Context, sessionId: String): File =
        File(ctx.filesDir, "frame_selection_$sessionId.json")

    fun writeFrameSelection(
        ctx: Context,
        sessionId: String,
        type: String,
        frameColorId: String? = null,
        seasonId: String? = null,
        customDesignJson: String? = null,
    ) {
        val o = JSONObject().apply {
            put("type", type)
            put("frameColorId", frameColorId ?: JSONObject.NULL)
            put("seasonId", seasonId ?: JSONObject.NULL)
            put("customDesignJson", customDesignJson ?: JSONObject.NULL)
        }
        frameSelFile(ctx, sessionId).writeText(o.toString())
    }

    data class FrameSelectionData(
        val type: String,
        val frameColorId: String?,
        val seasonId: String?,
        val customDesignJson: String?,
    )

    fun readFrameSelection(ctx: Context, sessionId: String): FrameSelectionData? {
        val f = frameSelFile(ctx, sessionId)
        if (!f.exists()) return null
        return runCatching {
            val o = JSONObject(f.readText())
            FrameSelectionData(
                type = o.optString("type", "solid"),
                frameColorId = if (o.isNull("frameColorId")) null else o.optString("frameColorId"),
                seasonId = if (o.isNull("seasonId")) null else o.optString("seasonId"),
                customDesignJson = if (o.isNull("customDesignJson")) null else o.optString("customDesignJson"),
            )
        }.getOrNull()
    }

    fun write(ctx: Context, sessionId: String, params: PendingCollageParams) {
        val o = JSONObject().apply {
            put("filterId", params.filterId.name)
            put("frameColorId", params.frameColorId)
            put("themeId", params.themeId)
            put("text", params.text)
            put("showDate", params.showDate)
            put("order", JSONArray(params.order))
            put("textFontSize", params.textFontSize.toDouble())
            put("dateFontSize", params.dateFontSize.toDouble())
            put("captionFontName", params.captionFontName ?: JSONObject.NULL)
            if (params.captionColorRGB != null) {
                put("captionColorRGB", params.captionColorRGB)
            }
            put("frameBackgroundType", params.frameBackgroundType)
            put("seasonId", params.seasonId ?: JSONObject.NULL)
            put("customDesignJson", params.customDesignJson ?: JSONObject.NULL)
        }
        file(ctx, sessionId).writeText(o.toString())
    }

    fun read(ctx: Context, sessionId: String): PendingCollageParams? {
        val f = file(ctx, sessionId)
        if (!f.exists()) return null
        return runCatching {
            val o = JSONObject(f.readText())
            val orderJson = o.getJSONArray("order")
            val order = buildList { for (i in 0 until orderJson.length()) add(orderJson.getInt(i)) }
            PendingCollageParams(
                filterId = runCatching { FilterId.valueOf(o.getString("filterId")) }.getOrElse { FilterId.ORIGINAL },
                frameColorId = o.optString("frameColorId", "white"),
                themeId = o.optString("themeId", ""),
                text = o.optString("text", ""),
                showDate = o.optBoolean("showDate", false),
                order = order,
                textFontSize = o.optDouble("textFontSize", 16.0).toFloat(),
                dateFontSize = o.optDouble("dateFontSize", 16.0).toFloat(),
                captionFontName = if (!o.has("captionFontName") || o.isNull("captionFontName")) null
                else o.optString("captionFontName", "").takeIf { it.isNotEmpty() },
                captionColorRGB = if (o.has("captionColorRGB") && !o.isNull("captionColorRGB")) o.getLong("captionColorRGB")
                else null,
                frameBackgroundType = o.optString("frameBackgroundType", "solid"),
                seasonId = if (!o.has("seasonId") || o.isNull("seasonId")) null
                else o.optString("seasonId"),
                customDesignJson = if (!o.has("customDesignJson") || o.isNull("customDesignJson")) null
                else o.optString("customDesignJson"),
            )
        }.getOrNull()
    }

    fun delete(ctx: Context, sessionId: String) {
        file(ctx, sessionId).delete()
        frameSelFile(ctx, sessionId).delete()
    }

    fun serializeDesign(design: CustomFrameDesign): String {
        val o = JSONObject().apply {
            put("fillColorId", design.fillColorId)
            put("fillHex", design.fillHex ?: JSONObject.NULL)
            put("sourceSeason", design.sourceSeason ?: JSONObject.NULL)
            val arr = JSONArray()
            for (dec in design.decorations) {
                arr.put(serializeDecoration(dec))
            }
            put("decorations", arr)
        }
        return o.toString()
    }

    fun deserializeDesign(json: String): CustomFrameDesign? = runCatching {
        val o = JSONObject(json)
        val decoArr = o.optJSONArray("decorations") ?: JSONArray()
        val decorations = buildList {
            for (i in 0 until decoArr.length()) {
                deserializeDecoration(decoArr.getJSONObject(i))?.let { add(it) }
            }
        }
        CustomFrameDesign(
            fillColorId = o.optString("fillColorId", "white"),
            fillHex = if (o.isNull("fillHex")) null else o.optLong("fillHex"),
            sourceSeason = if (o.isNull("sourceSeason")) null else o.optString("sourceSeason"),
            decorations = decorations,
        )
    }.getOrNull()

    private fun serializeDecoration(d: CustomFrameDecoration): JSONObject = JSONObject().apply {
        put("id", d.id)
        put("posX", d.position.x.toDouble())
        put("posY", d.position.y.toDouble())
        put("scale", d.scale.toDouble())
        put("rotationRadians", d.rotationRadians.toDouble())
        put("fontName", d.fontName ?: JSONObject.NULL)
        when (val k = d.kind) {
            is CustomFrameDecoration.Kind.Text -> {
                put("kindType", "text")
                put("content", k.content)
                put("textColorARGB", k.textColorARGB)
                put("fontScale", k.fontScale.toDouble())
            }
            is CustomFrameDecoration.Kind.Emoji -> {
                put("kindType", "emoji")
                put("content", k.content)
            }
            is CustomFrameDecoration.Kind.Sticker -> {
                put("kindType", "sticker")
                put("assetId", k.assetId)
                put("colorRGB", k.colorRGB)
            }
        }
    }

    private fun deserializeDecoration(o: JSONObject): CustomFrameDecoration? = runCatching {
        val kind = when (o.optString("kindType")) {
            "text" -> CustomFrameDecoration.Kind.Text(
                content = o.getString("content"),
                textColorARGB = o.getLong("textColorARGB"),
                fontScale = o.optDouble("fontScale", 0.08).toFloat(),
            )
            "emoji" -> CustomFrameDecoration.Kind.Emoji(content = o.getString("content"))
            "sticker" -> CustomFrameDecoration.Kind.Sticker(
                assetId = o.getString("assetId"),
                colorRGB = o.getLong("colorRGB"),
            )
            else -> return@runCatching null
        }
        CustomFrameDecoration(
            id = o.optString("id", java.util.UUID.randomUUID().toString()),
            position = NormPoint(o.optDouble("posX", 0.5).toFloat(), o.optDouble("posY", 0.5).toFloat()),
            scale = o.optDouble("scale", 1.0).toFloat(),
            rotationRadians = o.optDouble("rotationRadians", 0.0).toFloat(),
            fontName = if (o.isNull("fontName")) null else o.optString("fontName"),
            kind = kind,
        )
    }.getOrNull()
}
