package org.assignment.troubleshooter.ai;

/** Turns text into a vector of numbers that represents its meaning. */
public interface Embedder {

    /** Returns the vector for the text. Throws EmbeddingException if the model fails. */
    double[] embed(String text);

    /** Name of the model. Vectors from different models must never be compared. */
    String modelId();
}
