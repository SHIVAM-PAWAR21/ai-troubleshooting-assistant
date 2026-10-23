package org.assignment.troubleshooter.dto;

import java.util.List;

/** The answer sent to the browser. sources, knownResolution and escalationTeam always come from the database. */
public record TroubleshootResponse(
        Status status,
        String summary,
        List<String> steps,
        String knownResolution,
        List<String> sources,
        String escalationTeam,
        List<String> followUpQuestions) {

    public enum Status {
        ANSWERED,
        NEED_MORE_INFORMATION,
        INSUFFICIENT_KNOWLEDGE
    }
}
