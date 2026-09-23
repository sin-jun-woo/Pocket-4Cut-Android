package com.pocket4cut.data.local

import com.pocket4cut.domain.model.ExportOperation
import com.pocket4cut.domain.model.ExportStatus
import com.pocket4cut.domain.model.CURRENT_SESSION_SCHEMA_VERSION
import com.pocket4cut.domain.model.InputSource
import com.pocket4cut.domain.model.PhotoAdjustments
import com.pocket4cut.domain.model.PhotoCrop
import com.pocket4cut.domain.model.PhotoRef
import com.pocket4cut.domain.model.ResultRecord
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.domain.model.SessionStage
import com.pocket4cut.domain.model.inferFrameTypeId
import org.json.JSONArray
import org.json.JSONObject

internal object SessionDocumentCodec {
    fun encode(document: SessionDocument): String = JSONObject().apply {
        put("schemaVersion", document.schemaVersion)
        put("sessionId", document.sessionId)
        put("revision", document.revision)
        put("createdAt", document.createdAt)
        put("updatedAt", document.updatedAt)
        put("captureCount", document.captureCount)
        put("selectedCount", document.selectedCount)
        put("frameTypeId", document.frameTypeId)
        put("inputSource", document.inputSource.name)
        put("stage", document.stage.name)
        put("photos", JSONArray().apply {
            document.photos.forEach { photo ->
                put(JSONObject().apply {
                    put("photoId", photo.photoId)
                    put("path", photo.path)
                    put("captureIndex", photo.captureIndex)
                    put("legacy", photo.legacy)
                })
            }
        })
        put("draft", JSONObject().apply {
            val draft = document.draft
            put("selectedPhotoIdsInOrder", JSONArray(draft.selectedPhotoIdsInOrder))
            put("adjustmentsByPhotoId", JSONObject().apply {
                draft.adjustmentsByPhotoId.forEach { (photoId, adjustment) ->
                    put(photoId, JSONObject().apply {
                        put("rotationDegrees", adjustment.rotationDegrees)
                        put("flipHorizontal", adjustment.flipHorizontal)
                        put("brightness", adjustment.brightness.toDouble())
                        put("contrast", adjustment.contrast.toDouble())
                        put("saturation", adjustment.saturation.toDouble())
                        put("crop", JSONObject().apply {
                            put("focusX", adjustment.crop.focusX.toDouble())
                            put("focusY", adjustment.crop.focusY.toDouble())
                            put("zoom", adjustment.crop.zoom.toDouble())
                        })
                    })
                }
            })
            put("layoutId", draft.layoutId)
            put("layoutVersion", draft.layoutVersion)
            put("themeId", draft.themeId)
            put("occasionThemeId", draft.occasionThemeId ?: JSONObject.NULL)
            put("occasionDesignVersion", draft.occasionDesignVersion ?: JSONObject.NULL)
            put("frameStep", draft.frameStep)
            put("frameColorId", draft.frameColorId)
            put("backgroundType", draft.backgroundType)
            put("filterId", draft.filterId)
            put("caption", draft.caption)
            put("showDate", draft.showDate)
            put("dateText", draft.dateText)
            put("textFontSize", draft.textFontSize.toDouble())
            put("dateFontSize", draft.dateFontSize.toDouble())
            put("captionFontName", draft.captionFontName ?: JSONObject.NULL)
            put("captionColorRgb", draft.captionColorRgb ?: JSONObject.NULL)
            put("seasonId", draft.seasonId ?: JSONObject.NULL)
            put("customDesignJson", draft.customDesignJson ?: JSONObject.NULL)
        })
        put("results", JSONArray().apply {
            document.results.forEach { result ->
                put(JSONObject().apply {
                    put("resultId", result.resultId)
                    put("sourceRevision", result.sourceRevision)
                    put("path", result.path)
                    put("width", result.width)
                    put("height", result.height)
                    put("createdAt", result.createdAt)
                    put("legacy", result.legacy)
                })
            }
        })
        put("exportOperations", JSONArray().apply {
            document.exportOperations.forEach { operation ->
                put(JSONObject().apply {
                    put("operationId", operation.operationId)
                    put("resultId", operation.resultId)
                    put("status", operation.status.name)
                    put("uri", operation.uri ?: JSONObject.NULL)
                    put("displayName", operation.displayName)
                    put("createdAt", operation.createdAt)
                    put("isCopy", operation.isCopy)
                })
            }
        })
    }.toString()

    fun decode(raw: String): SessionDocument {
        val json = JSONObject(raw)
        val schemaVersion = json.getInt("schemaVersion")
        if (schemaVersion !in 1..CURRENT_SESSION_SCHEMA_VERSION) {
            throw UnsupportedSessionVersionException(schemaVersion)
        }
        val photosJson = json.getJSONArray("photos")
        val photos = (0 until photosJson.length()).map { index ->
            photosJson.getJSONObject(index).let {
                PhotoRef(
                    photoId = it.getString("photoId"),
                    path = it.getString("path"),
                    captureIndex = it.getInt("captureIndex"),
                    legacy = it.getBoolean("legacy"),
                )
            }
        }
        val draftJson = json.getJSONObject("draft")
        if (schemaVersion >= 3 &&
            (!draftJson.has("occasionThemeId") || !draftJson.has("occasionDesignVersion"))
        ) {
            throw IllegalArgumentException("Schema v3 occasion fields are missing")
        }
        val adjustmentsJson = draftJson.getJSONObject("adjustmentsByPhotoId")
        val adjustments = buildMap {
            val keys = adjustmentsJson.keys()
            while (keys.hasNext()) {
                val photoId = keys.next()
                val item = adjustmentsJson.getJSONObject(photoId)
                val crop = item.optJSONObject("crop")
                put(photoId, PhotoAdjustments(
                    rotationDegrees = item.getInt("rotationDegrees"),
                    flipHorizontal = item.getBoolean("flipHorizontal"),
                    brightness = item.getDouble("brightness").toFloat(),
                    contrast = item.getDouble("contrast").toFloat(),
                    saturation = item.getDouble("saturation").toFloat(),
                    crop = crop?.let {
                        PhotoCrop(
                            focusX = it.getDouble("focusX").toFloat(),
                            focusY = it.getDouble("focusY").toFloat(),
                            zoom = it.getDouble("zoom").toFloat(),
                        )
                    } ?: PhotoCrop(),
                ))
            }
        }
        val draft = SessionDraft(
            selectedPhotoIdsInOrder = draftJson.getJSONArray("selectedPhotoIdsInOrder").strings(),
            adjustmentsByPhotoId = adjustments,
            layoutId = draftJson.getString("layoutId"),
            layoutVersion = draftJson.getInt("layoutVersion"),
            themeId = draftJson.optString("themeId", ""),
            occasionThemeId = if (schemaVersion >= 3) draftJson.nullableString("occasionThemeId") else null,
            occasionDesignVersion = if (schemaVersion >= 3 && !draftJson.isNull("occasionDesignVersion")) {
                draftJson.getInt("occasionDesignVersion")
            } else null,
            frameStep = draftJson.optString("frameStep", "choose"),
            frameColorId = draftJson.getString("frameColorId"),
            backgroundType = draftJson.getString("backgroundType"),
            filterId = draftJson.getString("filterId"),
            caption = draftJson.getString("caption"),
            showDate = draftJson.getBoolean("showDate"),
            dateText = draftJson.getString("dateText"),
            textFontSize = draftJson.getDouble("textFontSize").toFloat(),
            dateFontSize = draftJson.getDouble("dateFontSize").toFloat(),
            captionFontName = draftJson.nullableString("captionFontName"),
            captionColorRgb = if (draftJson.isNull("captionColorRgb")) null else draftJson.getLong("captionColorRgb"),
            seasonId = draftJson.nullableString("seasonId"),
            customDesignJson = draftJson.nullableString("customDesignJson"),
        )
        val resultsJson = json.getJSONArray("results")
        val results = (0 until resultsJson.length()).map { index ->
            resultsJson.getJSONObject(index).let {
                ResultRecord(
                    resultId = it.getString("resultId"),
                    sourceRevision = it.getLong("sourceRevision"),
                    path = it.getString("path"),
                    width = it.getInt("width"),
                    height = it.getInt("height"),
                    createdAt = it.getLong("createdAt"),
                    legacy = it.getBoolean("legacy"),
                )
            }
        }
        val operationsJson = json.getJSONArray("exportOperations")
        val operations = (0 until operationsJson.length()).map { index ->
            operationsJson.getJSONObject(index).let {
                ExportOperation(
                    operationId = it.getString("operationId"),
                    resultId = it.getString("resultId"),
                    status = ExportStatus.valueOf(it.getString("status")),
                    uri = it.nullableString("uri"),
                    displayName = it.getString("displayName"),
                    createdAt = it.getLong("createdAt"),
                    isCopy = it.optBoolean("isCopy", false),
                )
            }
        }
        val captureCount = json.getInt("captureCount")
        val selectedCount = json.getInt("selectedCount")
        val migratedFrameTypeId = inferFrameTypeId(captureCount, selectedCount)
        val frameTypeId = if (schemaVersion == 1) {
            migratedFrameTypeId.orEmpty()
        } else {
            json.getString("frameTypeId")
        }
        val decodedStage = SessionStage.valueOf(json.getString("stage"))
        return SessionDocument(
            // Older schemas are upgraded in memory and atomically written as v3 on the next normal update.
            schemaVersion = CURRENT_SESSION_SCHEMA_VERSION,
            sessionId = json.getString("sessionId"),
            revision = json.getLong("revision"),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
            captureCount = captureCount,
            selectedCount = selectedCount,
            frameTypeId = frameTypeId,
            inputSource = if (schemaVersion == 1) InputSource.CAMERA else
                InputSource.valueOf(json.getString("inputSource")),
            stage = if (schemaVersion == 1 && migratedFrameTypeId == null) {
                SessionStage.NEEDS_RECOVERY
            } else decodedStage,
            photos = photos,
            draft = draft,
            results = results,
            exportOperations = operations,
        )
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else getString(key)
}
