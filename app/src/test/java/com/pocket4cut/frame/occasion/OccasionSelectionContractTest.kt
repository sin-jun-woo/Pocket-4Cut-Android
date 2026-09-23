package com.pocket4cut.frame.occasion

import org.junit.Assert.assertEquals
import org.junit.Test

class OccasionSelectionContractTest {
    private val known = setOf("birthday", "graduation")

    @Test fun legacyAndBasicFramesHaveNoOccasionSelection() {
        assertEquals(
            OccasionSelectionState.NONE,
            OccasionSelectionContract.evaluate("solid", null, null, known),
        )
    }

    @Test fun completeKnownSelectionIsValid() {
        assertEquals(
            OccasionSelectionState.VALID,
            OccasionSelectionContract.evaluate(
                "occasion", "birthday",
                OccasionCatalogContract.SESSION_DESIGN_VERSION, known,
            ),
        )
    }

    @Test fun occasionSelectionRejectsSeasonAndCustomPayloads() {
        listOf(
            OccasionSelectionContract.evaluate(
                "occasion", "birthday", 1, known,
                seasonId = "winter",
            ),
            OccasionSelectionContract.evaluate(
                "occasion", "birthday", 1, known,
                customDesignJson = "{}",
            ),
            OccasionSelectionContract.evaluate(
                "occasion", "birthday", 1, known,
                seasonId = "",
                customDesignJson = "",
            ),
        ).forEach { assertEquals(OccasionSelectionState.NEEDS_RECOVERY, it) }
    }

    @Test fun partialUnknownAndFutureSelectionsNeedRecovery() {
        listOf(
            OccasionSelectionContract.evaluate("solid", "birthday", 1, known),
            OccasionSelectionContract.evaluate("occasion", null, 1, known),
            OccasionSelectionContract.evaluate("occasion", "unknown", 1, known),
            OccasionSelectionContract.evaluate("occasion", "birthday", 2, known),
            OccasionSelectionContract.evaluate("occasion", "birthday", null, known),
            OccasionSelectionContract.evaluate("solid", null, 1, known),
        ).forEach { assertEquals(OccasionSelectionState.NEEDS_RECOVERY, it) }
    }
}
