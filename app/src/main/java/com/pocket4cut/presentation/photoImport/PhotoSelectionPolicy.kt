package com.pocket4cut.presentation.photoImport

/** Validates a single picker callback before any external bytes are copied. */
internal object PhotoSelectionPolicy {
    fun evaluate(
        targetCount: Int,
        selectedUris: List<String>,
    ): PhotoSelectionDecision {
        require(targetCount > 0)
        val distinct = selectedUris.distinct()
        val duplicates = selectedUris.groupingBy { it }.eachCount()
            .filterValues { it > 1 }
            .keys
            .toList()
        // This layer can only see duplicates inside the current callback. The repository also
        // knows URIs already committed to the draft, so it owns the remaining-slot decision.
        // Reject here only when a fallback picker ignores the callback's configured maximum.
        return if (distinct.size > targetCount) {
            PhotoSelectionDecision.RejectedTooMany(
                availableCount = targetCount,
                receivedCount = distinct.size,
            )
        } else {
            PhotoSelectionDecision.Accepted(distinct, duplicates)
        }
    }
}

internal sealed interface PhotoSelectionDecision {
    data class Accepted(
        val uris: List<String>,
        val duplicateUris: List<String>,
    ) : PhotoSelectionDecision

    data class RejectedTooMany(
        val availableCount: Int,
        val receivedCount: Int,
    ) : PhotoSelectionDecision
}
