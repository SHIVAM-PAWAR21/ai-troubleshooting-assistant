package org.assignment.troubleshooter.service;

import org.assignment.troubleshooter.ai.LlmException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmReplyTest {

    private static final String GOOD =
            "{\"covered\": true, \"summary\": \"This may match KB-001.\", \"steps\": [\"Check logs\", \"Check the database\"]}";

    @Test
    void readsPlainJson() {
        LlmReply reply = LlmReply.parse(GOOD);
        assertTrue(reply.covered());
        assertEquals(2, reply.steps().size());
    }

    @Test
    void readsJsonInsideCodeFencesAndExtraText() {
        assertTrue(LlmReply.parse("```json\n" + GOOD + "\n```").covered());
        assertTrue(LlmReply.parse("Sure! Here you go:\n" + GOOD + "\nHope that helps.").covered());
    }

    @Test
    void notCoveredNeedsNoSteps() {
        assertFalse(LlmReply.parse("{\"covered\": false, \"summary\": \"\", \"steps\": []}").covered());
    }

    @Test
    void rejectsRepliesThatAreNotUsable() {
        assertThrows(LlmException.class, () -> LlmReply.parse("I cannot help with that."));
        assertThrows(LlmException.class, () -> LlmReply.parse(null));
        assertThrows(LlmException.class, () -> LlmReply.parse("{\"covered\": true, \"summary\": "));
        assertThrows(LlmException.class,
                () -> LlmReply.parse("{\"covered\": true, \"summary\": \"s\", \"steps\": []}"));
    }
}
