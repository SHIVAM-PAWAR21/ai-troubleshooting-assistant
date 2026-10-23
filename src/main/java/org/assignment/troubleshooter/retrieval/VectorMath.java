package org.assignment.troubleshooter.retrieval;

/** Cosine similarity: how closely two vectors point the same way. 1 = same meaning, 0 = unrelated, -1 = opposite. */
public final class VectorMath {

    private VectorMath() {
    }

    public static double cosine(double[] a, double[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("vector is null");
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException("vector sizes differ: " + a.length + " vs " + b.length);
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0; // a zero vector has no direction, so treat it as "no similarity"
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
