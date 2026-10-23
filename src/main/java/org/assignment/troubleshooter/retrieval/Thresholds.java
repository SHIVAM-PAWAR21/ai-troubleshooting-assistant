package org.assignment.troubleshooter.retrieval;

/**
 * Cut-offs used to decide the outcome.
 *
 * @param keywordMin    keyword score below this counts as "no keyword match"
 * @param keywordStrong keyword score at or above this is enough by itself to answer (e.g. exact error code)
 * @param cosineLow     cosine similarity below this counts as "not related"
 * @param cosineHigh    cosine similarity at or above this is enough by itself to answer
 */
public record Thresholds(double keywordMin, double keywordStrong, double cosineLow, double cosineHigh) {

    /** Provisional values. Change them after reading the grid printed by HybridEvalTest. */
    public static final Thresholds DEFAULT = new Thresholds(3.0, 8.0, 0.30, 0.50);
}
