package org.assignment.troubleshooter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spike only: proves the local MiniLM model loads and understands meaning. Delete after Phase 3. */
@SpringBootTest
class EmbeddingSpikeTest {

    @Autowired
    EmbeddingModel model;

    @Test
    void embedsAndComparesMeaning() {
        float[] query = model.embed("The server ran out of storage");
        float[] disk = model.embed("Server disk space full. No space left on device.");
        float[] coffee = model.embed("The coffee machine is broken");

        System.out.println("\n=== SPIKE: dimensions = " + query.length);
        System.out.printf("storage vs disk-full entry = %.3f%n", cosine(query, disk));
        System.out.printf("storage vs coffee machine  = %.3f%n", cosine(query, coffee));

        assertEquals(384, query.length);
        assertTrue(cosine(query, disk) > cosine(query, coffee));
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
