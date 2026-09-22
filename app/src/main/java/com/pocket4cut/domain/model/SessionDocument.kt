package com.pocket4cut.domain.model

/** Persisted session format. A photo ID survives selection, reordering and edits. */
data class SessionDocument(
    val schemaVersion: Int = 1,
    val sessionId: String,
    val revision: Long = 0,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val captureCount: Int,
    val selectedCount: Int,
    val stage: SessionStage = SessionStage.CAPTURE,
    val photos: List<PhotoRef> = emptyList(),
    val draft: SessionDraft = SessionDraft(),
    val results: List<ResultRecord> = emptyList(),
    val exportOperations: List<ExportOperation> = emptyList(),
)

enum class SessionStage { CAPTURE, SELECT, FRAME, EDIT, DETAIL, RESULT, NEEDS_RECOVERY, DELETED }

/** New paths are relative to the app-owned Pocket4Cut pictures directory. Legacy paths are absolute. */
data class PhotoRef(
    val photoId: String,
    val path: String,
    val captureIndex: Int,
    val legacy: Boolean = false,
)

data class PhotoAdjustments(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
)

data class SessionDraft(
    val selectedPhotoIdsInOrder: List<String> = emptyList(),
    val adjustmentsByPhotoId: Map<String, PhotoAdjustments> = emptyMap(),
    val layoutId: String = "",
    val layoutVersion: Int = 2,
    val themeId: String = "",
    val frameStep: String = "choose",
    val frameColorId: String = "white",
    val backgroundType: String = "solid",
    val filterId: String = "ORIGINAL",
    val caption: String = "",
    val showDate: Boolean = false,
    val dateText: String = "",
    val textFontSize: Float = 16f,
    val dateFontSize: Float = 16f,
    val captionFontName: String? = null,
    val captionColorRgb: Long? = null,
    val seasonId: String? = null,
    val customDesignJson: String? = null,
)

/** A result is immutable: another save appends a new record and writes a different file. */
data class ResultRecord(
    val resultId: String,
    val sourceRevision: Long,
    val path: String,
    val width: Int,
    val height: Int,
    val createdAt: Long,
    val legacy: Boolean = false,
)

enum class ExportStatus { PREPARED, INSERTED, COPIED, PUBLISHED, COMPLETED, NEEDS_RECOVERY, FAILED }

data class ExportOperation(
    val operationId: String,
    val resultId: String,
    val status: ExportStatus,
    val uri: String? = null,
    val displayName: String,
    val createdAt: Long,
    val isCopy: Boolean = false,
)
