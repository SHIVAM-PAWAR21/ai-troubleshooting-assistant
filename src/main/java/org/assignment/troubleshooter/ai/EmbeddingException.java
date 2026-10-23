package org.assignment.troubleshooter.ai;

/** Thrown when an embedding model cannot turn text into a vector. */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
