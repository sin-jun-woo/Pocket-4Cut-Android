package com.pocket4cut.presentation.edit

import android.content.Context
import com.pocket4cut.frame.FilterId
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PendingCollageParams(
    val filterId: FilterId,
    val frameColorId: String,
    val text: String,
    val showDate: Boolean,
    val order: List<Int>,
)

object PendingCollageStore {
    private fun file(ctx: Context, sessionId: String): File =
        File(ctx.filesDir, "pending_collage_$sessionId.json")

    fun write(ctx: Context, sessionId: String, params: PendingCollageParams) {
        val o = JSONObject().apply {
            put("filterId", params.filterId.name)
            put("frameColorId", params.frameColorId)
            put("text", params.text)
            put("showDate", params.showDate)
            put("order", JSONArray(params.order))
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
                text = o.optString("text", ""),
                showDate = o.optBoolean("showDate", false),
                order = order,
            )
        }.getOrNull()
    }

    fun delete(ctx: Context, sessionId: String) { file(ctx, sessionId).delete() }
}
