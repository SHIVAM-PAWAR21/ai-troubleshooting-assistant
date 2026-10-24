package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Tests the merge-and-decide logic with made-up scores. No database, model or Spring needed. */
class HybridCombineTest {

    private static final Thresholds T = Thresholds.DEFAULT;

    private static ScoredEntry s(String id, double score) {
        return new ScoredEntry(new KnowledgeEntry(id, "title", "symptoms", List.of(), List.of(), null, null, null, null), score);
    }

    @Test
    void exactErrorCodeAnswers() {
        HybridResult r = HybridRetrievalService.combine(
                List.of(s("A", 20), s("B", 3)), List.of(s("A", 0.70), s("B", 0.20)), T);
        assertEquals(RetrievalOutcome.ANSWER, r.outcome());
        assertEquals("A", r.ranked().get(0).entry().id());
    }

    @Test
    void unrelatedQuestionIsInsufficient() {
        HybridResult r = HybridRetrievalService.combine(
                List.of(), List.of(s("A", 0.15), s("B", 0.10)), T);
        assertEquals(RetrievalOutcome.INSUFFICIENT_KNOWLEDGE, r.outcome());
    }

    @Test
    void keywordTieAskForMoreInformation() {
        HybridResult r = HybridRetrievalService.combine(
                List.of(s("A", 5), s("B", 5), s("C", 5)),
                List.of(s("A", 0.42), s("B", 0.40), s("C", 0.39)), T);
        assertEquals(RetrievalOutcome.NEED_MORE_INFORMATION, r.outcome());
    }

    @Test
    void bothMethodsAgreeingAnswers() {
        HybridResult r = HybridRetrievalService.combine(
                List.of(s("A", 4), s("B", 1)), List.of(s("A", 0.45), s("B", 0.30)), T);
        assertEquals(RetrievalOutcome.ANSWER, r.outcome());
    }

    @Test
    void entryFoundByBothMethodsRanksAboveEntryFoundByOne() {
        HybridResult r = HybridRetrievalService.combine(
                List.of(s("B", 10)), List.of(s("A", 0.60), s("B", 0.50), s("C", 0.10)), T);
        assertEquals("B", r.ranked().get(0).entry().id());
    }

    @Test
    void worksWithKeywordsOnlyWhenSemanticSearchIsUnavailable() {
        HybridResult strong = HybridRetrievalService.combine(List.of(s("A", 20)), null, T);
        assertEquals(RetrievalOutcome.ANSWER, strong.outcome());
        assertFalse(strong.semanticUsed());

        HybridResult medium = HybridRetrievalService.combine(List.of(s("A", 5)), null, T);
        assertEquals(RetrievalOutcome.NEED_MORE_INFORMATION, medium.outcome());

        HybridResult none = HybridRetrievalService.combine(List.of(), null, T);
        assertEquals(RetrievalOutcome.INSUFFICIENT_KNOWLEDGE, none.outcome());
    }

    @Test
    void semanticOnlyAnswersWhenCloseEnough() {
        HybridResult r = HybridRetrievalService.decideSemanticOnly(List.of(s("A", 0.70), s("B", 0.20)));
        assertEquals(RetrievalOutcome.ANSWER, r.outcome());
        assertEquals("A", r.ranked().get(0).entry().id());
    }

    @Test
    void semanticOnlyRejectsWeakMatch() {
        HybridResult r = HybridRetrievalService.decideSemanticOnly(List.of(s("A", 0.20), s("B", 0.10)));
        assertEquals(RetrievalOutcome.INSUFFICIENT_KNOWLEDGE, r.outcome());
    }

    @Test
    void semanticOnlyWithNoVectorsIsUnavailable() {
        HybridResult r = HybridRetrievalService.decideSemanticOnly(List.of());
        assertEquals(RetrievalOutcome.INSUFFICIENT_KNOWLEDGE, r.outcome());
        assertFalse(r.semanticUsed());
    }

}
