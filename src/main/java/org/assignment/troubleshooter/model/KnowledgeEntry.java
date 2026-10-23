package org.assignment.troubleshooter.model;

import java.util.List;

/** One row of the knowledge_base table. embedding is null until the indexing step (Phase 3). */
public record KnowledgeEntry(
        String id,
        String title,
        String symptoms,
        List<String> keywords,
        List<String> diagnosticSteps,
        String knownResolution,
        String escalationTeam,
        double[] embedding) {
}
