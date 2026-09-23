package com.pocket4cut.presentation.photoImport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoSelectionPolicyTest {
    @Test
    fun rejectsWholeCallbackWhenFallbackReturnsMoreThanConfiguredMaximum() {
        val result = PhotoSelectionPolicy.evaluate(
            targetCount = 2,
            selectedUris = listOf("content://one", "content://two", "content://three"),
        )

        assertEquals(PhotoSelectionDecision.RejectedTooMany(2, 3), result)
    }

    @Test
    fun removesDuplicatesWithoutSpendingAnotherSlot() {
        val result = PhotoSelectionPolicy.evaluate(
            targetCount = 2,
            selectedUris = listOf("content://one", "content://one", "content://two"),
        )

        assertTrue(result is PhotoSelectionDecision.Accepted)
        result as PhotoSelectionDecision.Accepted
        assertEquals(listOf("content://one", "content://two"), result.uris)
        assertEquals(listOf("content://one"), result.duplicateUris)
    }

    @Test
    fun acceptsFewerPhotosSoUserCanAddTheRestLater() {
        val result = PhotoSelectionPolicy.evaluate(
            targetCount = 6,
            selectedUris = listOf("content://one", "content://two"),
        )

        assertEquals(
            PhotoSelectionDecision.Accepted(
                uris = listOf("content://one", "content://two"),
                duplicateUris = emptyList(),
            ),
            result,
        )
    }

    @Test
    fun leavesExistingDraftDuplicateDecisionToRepository() {
        val result = PhotoSelectionPolicy.evaluate(
            targetCount = 4,
            selectedUris = listOf("content://already-imported", "content://new"),
        )

        assertEquals(
            PhotoSelectionDecision.Accepted(
                uris = listOf("content://already-imported", "content://new"),
                duplicateUris = emptyList(),
            ),
            result,
        )
    }
}
