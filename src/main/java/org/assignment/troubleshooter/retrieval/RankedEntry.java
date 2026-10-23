package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.KnowledgeEntry;

/**
 * One KB entry in the combined ranking, with the signals behind its position.
 *
 * @param rrf          combined rank-fusion score (higher is better)
 * @param keywordScore raw keyword score, 0 if the entry had no keyword match
 * @param cosine       raw cosine similarity, null if semantic search was not available for this entry
 */
public record RankedEntry(KnowledgeEntry entry, double rrf, double keywordScore, Double cosine) {
}
