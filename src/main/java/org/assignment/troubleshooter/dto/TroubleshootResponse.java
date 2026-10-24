
package org.assignment.troubleshooter.dto;

import java.util.List;

/**
 * The answer sent to the browser. sources, knownResolution and escalationTeam always come from the database.
 * matches lists the three closest entries with their scores (used by the "How this was found" panel).
 */
public record TroubleshootResponse(
        Status status,
        String summary,
        List<String> steps,
        String knownResolution,
        List<String> sources,
        String escalationTeam,
        List<String> followUpQuestions,
        List<Match> matches) {

    /** One of the closest KB entries with the scores behind it, so the page can show how the match was found. */
    public record Match(String id, String title, double keywordScore, Double cosine) {
    }

    public enum Status {
        ANSWERED,
        NEED_MORE_INFORMATION,
        INSUFFICIENT_KNOWLEDGE
    }
}