package com.pocket4cut.frame

import com.pocket4cut.data.local.SessionDocumentRepository
import com.pocket4cut.domain.model.PhotoAdjustments
import com.pocket4cut.domain.model.SessionDocument
import com.pocket4cut.presentation.edit.PendingCollageStore
import com.pocket4cut.presentation.navigation.FrameType

/** Immutable input captured from one persisted draft revision before preview or export. */
data class RenderSnapshot(
    val sessionId: String,
    val revision: Long,
    val photoIdsInOrder: List<String>,
    val imagePathsInOrder: List<String>,
    val adjustmentsByPhotoId: Map<String, PhotoAdjustments>,
    val cropTransformsInOrder: List<PhotoCropTransform>,
    val frameStyle: FrameStyle,
    val theme: FrameTheme,
    val frameColor: FrameColor,
    val filterId: FilterId,
    val caption: String,
    val dateText: String?,
    val textFontSize: Float,
    val dateFontSize: Float,
    val captionFontName: String?,
    val captionColorRgb: Long?,
    val customFrameDesign: CustomFrameDesign?,
    val layoutVersion: Int,
    val occasionThemeId: String?,
    val occasionDesignVersion: Int?,
) {
    companion object {
        fun from(
            document: SessionDocument,
            repository: SessionDocumentRepository,
            frameType: FrameType,
        ): RenderSnapshot {
            val draft = document.draft
            require(draft.selectedPhotoIdsInOrder.size == document.selectedCount)
            val photoById = document.photos.associateBy { it.photoId }
            val paths = draft.selectedPhotoIdsInOrder.map { id ->
                val photo = photoById[id] ?: error("Selected photo is missing")
                repository.resolvePhotoPath(photo).also { require(it.isFile) }.absolutePath
            }
            val layout = FrameLayoutId.valueOf(draft.layoutId)
            val theme = FrameCatalog.themes(frameType).first { it.id == draft.themeId }
            val cropTransforms = draft.selectedPhotoIdsInOrder.map { id ->
                val adjustment = draft.adjustmentsByPhotoId[id] ?: PhotoAdjustments()
                PhotoCropTransform(
                    crop = adjustment.crop,
                    quarterTurnsClockwise = ((adjustment.rotationDegrees / 90) % 4 + 4) % 4,
                    flipHorizontal = adjustment.flipHorizontal,
                )
            }
            return RenderSnapshot(
                sessionId = document.sessionId,
                revision = document.revision,
                photoIdsInOrder = draft.selectedPhotoIdsInOrder.toList(),
                imagePathsInOrder = paths,
                adjustmentsByPhotoId = draft.adjustmentsByPhotoId.toMap(),
                cropTransformsInOrder = cropTransforms,
                frameStyle = FrameLayouts.byId(layout),
                theme = theme,
                frameColor = FrameColors.byId(draft.frameColorId),
                filterId = FilterId.valueOf(draft.filterId),
                caption = draft.caption,
                dateText = draft.dateText.takeIf { draft.showDate },
                textFontSize = draft.textFontSize,
                dateFontSize = draft.dateFontSize,
                captionFontName = draft.captionFontName,
                captionColorRgb = draft.captionColorRgb,
                customFrameDesign = draft.customDesignJson?.let(PendingCollageStore::deserializeDesign),
                layoutVersion = draft.layoutVersion,
                occasionThemeId = draft.occasionThemeId,
                occasionDesignVersion = draft.occasionDesignVersion,
            )
        }
    }
}
