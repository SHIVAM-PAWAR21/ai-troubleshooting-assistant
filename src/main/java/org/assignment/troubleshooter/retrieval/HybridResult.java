package org.assignment.troubleshooter.retrieval;

import java.util.List;

/**
 * Result of hybrid retrieval.
 *
 * @param outcome      what to do with the question
 * @param ranked       KB entries, best first
 * @param semanticUsed false if semantic search failed and only keywords were used
 */
public record HybridResult(RetrievalOutcome outcome, List<RankedEntry> ranked, boolean semanticUsed) {
}
