package org.assignment.troubleshooter.ai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/** Embedder that runs the all-MiniLM-L6-v2 model on this machine (384 numbers per text). */
@Component
public class LocalMiniLmEmbedder implements Embedder {

    public static final String MODEL_ID = "all-MiniLM-L6-v2";

    private final EmbeddingModel model;

    public LocalMiniLmEmbedder(EmbeddingModel model) {
        this.model = model;
    }

    @Override
    public double[] embed(String text) {
        try {
            float[] v = model.embed(text);
            double[] result = new double[v.length];
            for (int i = 0; i < v.length; i++) {
                result[i] = v[i];
            }
            return result;
        } catch (RuntimeException e) {
            throw new EmbeddingException("MiniLM embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String modelId() {
        return MODEL_ID;
    }
}
