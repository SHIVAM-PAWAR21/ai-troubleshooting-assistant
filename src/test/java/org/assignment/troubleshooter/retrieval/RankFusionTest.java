package org.assignment.troubleshooter.retrieval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assignment.troubleshooter.retrieval.OutcomePolicyTest.s;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Tests how the two rankings are merged. No database, model or Spring needed. */
class RankFusionTest {

    private final RankFusion fusion = new RankFusion();

    @Test
    void entryFoundByBothMethodsRanksAboveEntryFoundByOne() {
        List<RankedEntry> ranked = fusion.fuse(List.of(s("B", 10)), List.of(s("A", 0.60), s("B", 0.50), s("C", 0.10)));
        assertEquals("B", ranked.get(0).entry().id());
        assertEquals(10, ranked.get(0).keywordScore());
        assertEquals(0.50, ranked.get(0).cosine());
    }

    @Test
    void keepsTheRawScoresFromEachSide() {
        List<RankedEntry> ranked = fusion.fuse(List.of(s("A", 20), s("B", 3)), List.of(s("A", 0.70), s("C", 0.20)));
        assertEquals(List.of("A", "B", "C"), ranked.stream().map(r -> r.entry().id()).toList());
        assertNull(ranked.get(1).cosine());          // B was only found by keywords
        assertEquals(0, ranked.get(2).keywordScore()); // C was only found by meaning
    }

    @Test
    void worksWithoutSemanticResults() {
        List<RankedEntry> ranked = fusion.fuse(List.of(s("A", 5), s("B", 4)), List.of());
        assertEquals(List.of("A", "B"), ranked.stream().map(r -> r.entry().id()).toList());
    }
}
