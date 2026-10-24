package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.assignment.troubleshooter.model.Status;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests the decision rules with made-up scores. No database, model or Spring needed. */
class OutcomePolicyTest {

    private final OutcomePolicy policy = new OutcomePolicy(Thresholds.DEFAULT);

    static ScoredEntry s(String id, double score) {
        return new ScoredEntry(new KnowledgeEntry(id, "title", "symptoms", List.of(), List.of(), null, null, null, null), score);
    }

    @Test
    void exactErrorCodeAnswers() {
        assertEquals(Status.ANSWERED, policy.decideHybrid(
                List.of(s("A", 20), s("B", 3)), List.of(s("A", 0.70), s("B", 0.20))));
    }

    @Test
    void unrelatedQuestionIsInsufficient() {
        assertEquals(Status.INSUFFICIENT_KNOWLEDGE, policy.decideHybrid(
                List.of(), List.of(s("A", 0.15), s("B", 0.10))));
    }

    @Test
    void keywordTieAsksForMoreInformation() {
        assertEquals(Status.NEED_MORE_INFORMATION, policy.decideHybrid(
                List.of(s("A", 5), s("B", 5), s("C", 5)),
                List.of(s("A", 0.42), s("B", 0.40), s("C", 0.39))));
    }

    @Test
    void bothMethodsAgreeingAnswers() {
        assertEquals(Status.ANSWERED, policy.decideHybrid(
                List.of(s("A", 4), s("B", 1)), List.of(s("A", 0.45), s("B", 0.30))));
    }

    @Test
    void worksWithKeywordsOnlyWhenSemanticSearchIsUnavailable() {
        assertEquals(Status.ANSWERED, policy.decideHybrid(List.of(s("A", 20)), List.of()));
        assertEquals(Status.NEED_MORE_INFORMATION, policy.decideHybrid(List.of(s("A", 5)), List.of()));
        assertEquals(Status.INSUFFICIENT_KNOWLEDGE, policy.decideHybrid(List.of(), List.of()));
    }

    @Test
    void semanticOnlyAnswersWhenCloseEnough() {
        assertEquals(Status.ANSWERED, policy.decideSemanticOnly(List.of(s("A", 0.70), s("B", 0.20))));
    }

    @Test
    void semanticOnlyRejectsWeakMatch() {
        assertEquals(Status.INSUFFICIENT_KNOWLEDGE, policy.decideSemanticOnly(List.of(s("A", 0.20), s("B", 0.10))));
    }

    @Test
    void semanticOnlyWithNoVectorsIsInsufficient() {
        assertEquals(Status.INSUFFICIENT_KNOWLEDGE, policy.decideSemanticOnly(List.of()));
    }
}
