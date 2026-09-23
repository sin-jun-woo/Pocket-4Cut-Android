package com.pocket4cut.presentation.result

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultActionStateTest {
    @Test
    fun loadingAndUnavailableLinksDisableBothActions() {
        val loading = ResultActionState(
            linkState = ResultLinkLoadState.LOADING,
            operation = ResultOperation.IDLE,
            isSaved = false,
        )
        val unavailable = loading.copy(linkState = ResultLinkLoadState.UNAVAILABLE)

        assertTrue(loading.isPreparing)
        assertFalse(loading.saveEnabled)
        assertFalse(loading.shareEnabled)
        assertFalse(unavailable.isPreparing)
        assertFalse(unavailable.saveEnabled)
        assertFalse(unavailable.shareEnabled)
    }

    @Test
    fun readyLinkAllowsOnlyIdleOperations() {
        val idle = ResultActionState(
            linkState = ResultLinkLoadState.READY,
            operation = ResultOperation.IDLE,
            isSaved = false,
        )

        assertTrue(idle.saveEnabled)
        assertTrue(idle.shareEnabled)

        ResultOperation.entries
            .filterNot { it == ResultOperation.IDLE }
            .forEach { operation ->
                val busy = idle.copy(operation = operation)
                assertFalse(busy.saveEnabled)
                assertFalse(busy.shareEnabled)
            }
    }

    @Test
    fun verifiedExistingCopyDisablesSaveButKeepsShareAvailable() {
        val saved = ResultActionState(
            linkState = ResultLinkLoadState.READY,
            operation = ResultOperation.IDLE,
            isSaved = true,
        )

        assertFalse(saved.saveEnabled)
        assertTrue(saved.shareEnabled)
    }
}
