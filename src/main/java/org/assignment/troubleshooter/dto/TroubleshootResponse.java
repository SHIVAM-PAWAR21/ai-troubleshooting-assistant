package org.assignment.troubleshooter.dto;

import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.assignment.troubleshooter.model.Status;

import java.util.List;

/**
 * The answer sent to the browser. sources, knownResolution and escalationTeam always come from the database.
 * matches lists the three closest entries with their scores (used by the "How this was found" panel).
 * Build it with answered(), needMoreInfo() or notFound(), one per outcome.
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

    private static final List<String> FOLLOW_UPS = List.of(
            "Which service or application is affected?",
            "What exact error message or status code do you see?",
            "When did it start, and did anything change before it (deployment, configuration, certificate)?");

    /** One of the closest KB entries with the scores behind it, so the page can show how the match was found. */
    public record Match(String id, String title, double keywordScore, Double cosine) {
    }

    /** A KB entry matched: the steps to try, plus the resolution and team from that entry. */
    public static TroubleshootResponse answered(String summary, List<String> steps, KnowledgeEntry entry,
                                                List<Match> matches) {
        return new TroubleshootResponse(Status.ANSWERED, summary, steps, entry.knownResolution(),
                List.of(entry.label()), entry.escalationTeam(), List.of(), matches);
    }

    /** Several entries may match: show them as candidates and ask the follow-up questions. */
    public static TroubleshootResponse needMoreInfo(String summary, List<String> candidates, List<Match> matches) {
        return new TroubleshootResponse(Status.NEED_MORE_INFORMATION, summary, List.of(), null,
                candidates, null, FOLLOW_UPS, matches);
    }

    /** Nothing in the KB fits: say so and ask the follow-up questions. */
    public static TroubleshootResponse notFound(String summary, List<Match> matches) {
        return new TroubleshootResponse(Status.INSUFFICIENT_KNOWLEDGE, summary, List.of(), null,
                List.of(), null, FOLLOW_UPS, matches);
    }
}
