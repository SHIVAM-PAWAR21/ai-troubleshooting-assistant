package org.assignment.troubleshooter.model;

import java.util.List;

/** One row of the knowledge_base table. embedding and embeddingModel are null until the entry is indexed. */
public record KnowledgeEntry(
        String id,
        String title,
        String symptoms,
        List<String> keywords,
        List<String> diagnosticSteps,
        String knownResolution,
        String escalationTeam,
        double[] embedding,
        String embeddingModel) {

    /** How the entry is shown to the user, e.g. "KB-001 - API returns HTTP 500". */
    public String label() {
        return id + " - " + title;
    }

    /** The text that gets embedded. The steps are left out on purpose: they add noise to matching. */
    public String embeddingText() {
        return title + ". " + symptoms + " Keywords: " + String.join(", ", keywords);
    }

    /** True if the stored vector came from this model. Vectors from different models must never be compared. */
    public boolean hasEmbeddingFrom(String modelId) {
        return embedding != null && modelId.equals(embeddingModel);
    }
}
