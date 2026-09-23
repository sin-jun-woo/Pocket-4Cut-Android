package com.pocket4cut.presentation.photoImport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoImportUiStateTest {
    private val completePhotos = listOf(
        ImportedPhotoItem("first", "/first.jpg"),
        ImportedPhotoItem("second", "/second.jpg"),
    )

    @Test
    fun dismissibleErrorNoticeDoesNotBlockACompleteSelection() {
        val state = PhotoImportUiState(
            isInitialized = true,
            photos = completePhotos,
            message = "중복 사진은 추가하지 않았습니다.",
            isMessageError = true,
        )

        assertTrue(state.canContinue(requiredCount = 2))
        val dismissed = state.dismissMessage()
        assertNull(dismissed.message)
        assertFalse(dismissed.isMessageError)
        assertTrue(dismissed.canContinue(requiredCount = 2))
    }

    @Test
    fun dismissingPresentationStateDoesNotClearARecoveryBlock() {
        val state = PhotoImportUiState(
            isInitialized = true,
            photos = completePhotos,
            message = "가져오기 복구를 완료하지 못했습니다.",
            isMessageError = true,
            hasBlockingRecoveryError = true,
        )

        val dismissed = state.dismissMessage()

        assertEquals(state.message, dismissed.message)
        assertTrue(dismissed.isMessageError)
        assertTrue(dismissed.hasBlockingRecoveryError)
        assertFalse(dismissed.canContinue(requiredCount = 2))
    }

    @Test
    fun persistedEditFailureBlocksContinuingWithStalePhotoState() {
        val state = PhotoImportUiState(
            isInitialized = true,
            photos = completePhotos,
            message = "사진 순서를 저장하지 못했습니다.",
            isMessageError = true,
            hasUnacknowledgedEditFailure = true,
        )

        assertFalse(state.canContinue(requiredCount = 2))
        val acknowledged = state.dismissMessage()
        assertTrue(acknowledged.canContinue(requiredCount = 2))
        assertFalse(acknowledged.hasUnacknowledgedEditFailure)
    }
}
