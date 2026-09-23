package com.pocket4cut.frame.occasion

import com.pocket4cut.domain.model.SessionDraft
import com.pocket4cut.frame.FrameCatalog
import com.pocket4cut.frame.RenderSnapshot
import com.pocket4cut.presentation.navigation.FrameType
import com.pocket4cut.presentation.navigation.withOccasionSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test fun applyingOccasionPersistsTheSelectedBaseFrameTheme() {
        val baseFrameTheme = FrameCatalog.themes(FrameType.TWO_CUT).last()

        val selected = SessionDraft().withOccasionSelection("birthday", baseFrameTheme)

        assertEquals(baseFrameTheme.id, selected.themeId)
        assertEquals("birthday", selected.occasionThemeId)
        assertEquals(OccasionCatalogContract.SESSION_DESIGN_VERSION, selected.occasionDesignVersion)
    }

    @Test fun renderThemeCompatibilityOnlyDefaultsBlankCurrentOccasionDrafts() {
        val defaultTheme = FrameCatalog.themes(FrameType.TWO_CUT).first()
        val affectedDraft = SessionDraft(
            themeId = "",
            frameStep = "occasion",
            backgroundType = "occasion",
            occasionThemeId = "birthday",
            occasionDesignVersion = OccasionCatalogContract.SESSION_DESIGN_VERSION,
        )

        assertEquals(defaultTheme, RenderSnapshot.resolveFrameTheme(affectedDraft, FrameType.TWO_CUT))

        val unknownFailure = runCatching {
            RenderSnapshot.resolveFrameTheme(
                affectedDraft.copy(themeId = "two_removed_theme"),
                FrameType.TWO_CUT,
            )
        }.exceptionOrNull()
        assertTrue(unknownFailure is IllegalStateException)
        assertTrue(unknownFailure?.message?.contains("two_removed_theme") == true)

        val unrelatedBlankFailure = runCatching {
            RenderSnapshot.resolveFrameTheme(SessionDraft(themeId = ""), FrameType.TWO_CUT)
        }.exceptionOrNull()
        assertTrue(unrelatedBlankFailure is IllegalStateException)
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
