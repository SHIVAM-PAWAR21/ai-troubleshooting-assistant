package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.KnowledgeEntry;

/** A KB entry with the score a retrieval method gave it for one query. */
public record ScoredEntry(KnowledgeEntry entry, double score) {
}
