package org.assignment.troubleshooter.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * What we ask the chat model to return. It only writes the summary and the steps.
 * The source, escalation team and known resolution always come from the database.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmReply(boolean covered, String summary, List<String> steps) {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** Reads the model's reply. Models sometimes add text or code fences, so we cut out the {...} part first. */
    public static LlmReply parse(String raw) {
        int start = raw == null ? -1 : raw.indexOf('{');
        int end = raw == null ? -1 : raw.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("The reply is not JSON");
        }
        LlmReply reply;
        try {
            reply = MAPPER.readValue(raw.substring(start, end + 1), LlmReply.class);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("The reply is not valid JSON");
        }
        if (reply.covered() && (reply.summary() == null || reply.summary().isBlank()
                || reply.steps() == null || reply.steps().isEmpty())) {
            throw new IllegalArgumentException("The reply has no summary or steps");
        }
        return reply;
    }
}
