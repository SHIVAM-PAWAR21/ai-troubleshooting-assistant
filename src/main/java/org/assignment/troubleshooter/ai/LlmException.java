package org.assignment.troubleshooter.ai;

/** Thrown when the chat model cannot be called or its reply cannot be used. */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
