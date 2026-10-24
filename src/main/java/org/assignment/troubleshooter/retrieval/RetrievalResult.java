package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.Status;

import java.util.List;

/**
 * Result of one search.
 *
 * @param outcome what to do with the question
 * @param ranked  KB entries, best first
 */
public record RetrievalResult(Status outcome, List<RankedEntry> ranked) {
}
