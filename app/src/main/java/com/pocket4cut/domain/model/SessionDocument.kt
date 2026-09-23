package com.pocket4cut.domain.model

/** Persisted session format. A photo ID survives selection, reordering and edits. */
data class SessionDocument(
    val schemaVersion: Int = CURRENT_SESSION_SCHEMA_VERSION,
    val sessionId: String,
    val revision: Long = 0,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val captureCount: Int,
    val selectedCount: Int,
    /** 2, 4 or 6. This is the source of truth; captureCount is retained for old data. */
    val frameTypeId: String = inferFrameTypeId(captureCount, selectedCount).orEmpty(),
    val inputSource: InputSource = InputSource.CAMERA,
    val stage: SessionStage = SessionStage.CAPTURE,
    val photos: List<PhotoRef> = emptyList(),
    val draft: SessionDraft = SessionDraft(),
    val results: List<ResultRecord> = emptyList(),
    val exportOperations: List<ExportOperation> = emptyList(),
)

const val CURRENT_SESSION_SCHEMA_VERSION = 3

enum class InputSource { CAMERA, ALBUM }

enum class SessionStage { CAPTURE, IMPORT, SELECT, FRAME, EDIT, DETAIL, RESULT, NEEDS_RECOVERY, DELETED }

/** Maps only combinations that existed in schema v1. Unknown pairs require explicit recovery. */
fun inferFrameTypeId(captureCount: Int, selectedCount: Int): String? = when (captureCount to selectedCount) {
    4 to 2 -> "2"
    8 to 4 -> "4"
    10 to 6 -> "6"
    else -> null
}

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
    val crop: PhotoCrop = PhotoCrop(),
)

/** Non-destructive focus point and scale in EXIF-normalized source coordinates. */
data class PhotoCrop(
    val focusX: Float = 0.5f,
    val focusY: Float = 0.5f,
    val zoom: Float = 1f,
)

data class SessionDraft(
    val selectedPhotoIdsInOrder: List<String> = emptyList(),
    val adjustmentsByPhotoId: Map<String, PhotoAdjustments> = emptyMap(),
    val layoutId: String = "",
    val layoutVersion: Int = 2,
    val themeId: String = "",
    /** Stable ID from the bundled occasion catalog. Null for the legacy/basic frame flows. */
    val occasionThemeId: String? = null,
    /** Catalog design version used to resolve [occasionThemeId]. */
    val occasionDesignVersion: Int? = null,
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
