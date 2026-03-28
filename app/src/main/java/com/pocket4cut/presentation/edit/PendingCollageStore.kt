package com.pocket4cut.presentation.edit

import android.content.Context
import com.pocket4cut.frame.RenderFilter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PendingCollageParams(
    val filter: RenderFilter,
    val text: String,
    val showDate: Boolean,
    val order: List<Int>,
)

object PendingCollageStore {
    private fun file(ctx: Context, sessionId: String): File =
        File(ctx.filesDir, "pending_collage_$sessionId.json")

    fun write(ctx: Context, sessionId: String, params: PendingCollageParams) {
        val o = JSONObject().apply {
            put("filter", params.filter.name)
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
            val order = buildList {
                for (i in 0 until orderJson.length()) {
                    add(orderJson.getInt(i))
                }
            }
            val filterName = o.getString("filter")
            val filter = runCatching { RenderFilter.valueOf(filterName) }.getOrElse { RenderFilter.SOFT }
            PendingCollageParams(
                filter = filter,
                text = o.optString("text", ""),
                showDate = o.optBoolean("showDate", true),
                order = order,
            )
        }.getOrNull()
    }

    fun delete(ctx: Context, sessionId: String) {
        file(ctx, sessionId).delete()
    }
}
