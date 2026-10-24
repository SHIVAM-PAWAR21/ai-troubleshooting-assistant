package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.Status;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decides answer / ask for more / not in the KB from the raw scores.
 * The fused RRF score is not used here: it only says "better than the others", never "good enough".
 */
@Component
public class OutcomePolicy {

    private final Thresholds t;

    public OutcomePolicy() {
        this(Thresholds.DEFAULT);
    }

    public OutcomePolicy(Thresholds thresholds) {
        this.t = thresholds;
    }

    /**
     * @param keywordResults  entries with a keyword score above 0, best first
     * @param semanticResults entries with a cosine score, best first; empty if semantic search was not available
     */
    public Status decideHybrid(List<ScoredEntry> keywordResults, List<ScoredEntry> semanticResults) {
        boolean semanticUsed = !semanticResults.isEmpty();
        double topKeyword = keywordResults.isEmpty() ? 0 : keywordResults.get(0).score();
        double topCosine = semanticUsed ? semanticResults.get(0).score() : 0;

        // Rule 1: neither method found anything related.
        boolean weakKeyword = topKeyword < t.keywordMin();
        boolean weakSemantic = !semanticUsed || topCosine < t.cosineLow();
        if (weakKeyword && weakSemantic) {
            return Status.INSUFFICIENT_KNOWLEDGE;
        }

        // Rule 2: one method is sure on its own (e.g. an exact error code, or a very close meaning).
        if (topKeyword >= t.keywordStrong() || (semanticUsed && topCosine >= t.cosineHigh())) {
            return Status.ANSWERED;
        }

        // Rule 3: both methods point at the same entry, neither is weak, and keywords don't tie
        // (a tie means the question is vague, e.g. "the API is not working").
        boolean agree = !keywordResults.isEmpty() && semanticUsed
                && keywordResults.get(0).entry().id().equals(semanticResults.get(0).entry().id());
        boolean tied = keywordResults.size() >= 2 && topKeyword - keywordResults.get(1).score() < 1.0;
        if (agree && !weakKeyword && !weakSemantic && !tied) {
            return Status.ANSWERED;
        }

        // Rule 4: something may match, but not clearly.
        return Status.NEED_MORE_INFORMATION;
    }

    /** Semantic-only mode has one rule: answer if the closest entry is close enough in meaning. */
    public Status decideSemanticOnly(List<ScoredEntry> semanticResults) {
        boolean closeEnough = !semanticResults.isEmpty() && semanticResults.get(0).score() >= t.semanticOnlyMin();
        return closeEnough ? Status.ANSWERED : Status.INSUFFICIENT_KNOWLEDGE;
    }
}
