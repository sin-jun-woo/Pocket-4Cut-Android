package com.pocket4cut.data.local

import android.content.Context
import com.pocket4cut.domain.model.PhotoSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SessionRepository(
    private val context: Context,
) {
    private val file: File = File(context.filesDir, "sessions.json")

    suspend fun upsert(session: PhotoSession) = withContext(Dispatchers.IO) {
        val sessions = readAllInternal().toMutableList()
        val idx = sessions.indexOfFirst { it.id == session.id }
        if (idx >= 0) sessions[idx] = session else sessions.add(session)
        writeAllInternal(sessions)
    }

    suspend fun getAll(): List<PhotoSession> = withContext(Dispatchers.IO) {
        readAllInternal()
            .filter { !it.finalImagePath.isNullOrBlank() }
            .sortedByDescending { it.createdAt }
    }

    suspend fun getById(id: String): PhotoSession? = withContext(Dispatchers.IO) {
        readAllInternal().firstOrNull { it.id == id }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val sessions = readAllInternal().filterNot { it.id == id }
        writeAllInternal(sessions)
    }

    private fun readAllInternal(): List<PhotoSession> {
        if (!file.exists()) return emptyList()
        val raw = file.readText()
        if (raw.isBlank()) return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).mapNotNull { i ->
            runCatching { arr.getJSONObject(i).toSession() }.getOrNull()
        }
    }

    private fun writeAllInternal(list: List<PhotoSession>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        file.writeText(arr.toString())
    }
}

private fun JSONObject.toSession(): PhotoSession {
    val imagePaths = getJSONArray("imagePaths").toStringList()
    val selectedIndexes = getJSONArray("selectedIndexes").toIntList()
    return PhotoSession(
        id = getString("id"),
        captureCount = getInt("captureCount"),
        selectedCount = getInt("selectedCount"),
        imagePaths = imagePaths,
        selectedIndexes = selectedIndexes,
        frameId = getString("frameId"),
        finalImagePath = optString("finalImagePath").takeIf { it.isNotBlank() },
        createdAt = getLong("createdAt"),
    )
}

private fun PhotoSession.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("captureCount", captureCount)
        .put("selectedCount", selectedCount)
        .put("imagePaths", JSONArray(imagePaths))
        .put("selectedIndexes", JSONArray(selectedIndexes))
        .put("frameId", frameId)
        .put("finalImagePath", finalImagePath ?: "")
        .put("createdAt", createdAt)

private fun JSONArray.toStringList(): List<String> =
    (0 until length()).mapNotNull { i -> optString(i).takeIf { it.isNotBlank() } }

private fun JSONArray.toIntList(): List<Int> =
    (0 until length()).mapNotNull { i -> optInt(i).takeIf { it >= 0 } }

