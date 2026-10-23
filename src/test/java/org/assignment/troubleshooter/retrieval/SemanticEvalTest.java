package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.assignment.troubleshooter.repository.KbRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase 3: measures semantic-only retrieval (MiniLM + cosine similarity) on the same questions as the keyword test. */
@SpringBootTest
class SemanticEvalTest {

    /** Provisional cut-off. Change it after reading the sweep table printed by this test. */
    static final double THRESHOLD = 0.35;

    @Autowired
    KbRepository repo;

    @Autowired
    KbIndexer indexer;

    @Autowired
    SemanticRetrievalService semantic;

    @Test
    void semanticOnlyEvaluation() throws Exception {
        indexer.indexAll(); // skips entries that already have a vector
        List<KnowledgeEntry> kb = repo.findAll();
        assertEquals(15, kb.size());
        assertTrue(kb.stream().allMatch(e -> e.embedding() != null), "some entries have no embedding");

        List<EvalSupport.EvalQuestion> questions = EvalSupport.load();
        Map<String, List<ScoredEntry>> results = new HashMap<>();
        for (EvalSupport.EvalQuestion q : questions) {
            results.put(q.id(), semantic.search(q.question(), kb)); // embed each question once
        }

        System.out.println("\n=== SEMANTIC-ONLY EVALUATION (threshold " + THRESHOLD + ") ===");
        for (EvalSupport.EvalQuestion q : questions) {
            System.out.printf("%-3s %-12s expected=%-16s top3: %-32s | %s%n",
                    q.id(), q.type(), q.expected(), EvalSupport.top3(results.get(q.id())), q.question());
        }

        System.out.println("\nBy type at threshold " + THRESHOLD + ":");
        for (String type : List.of("direct", "paraphrase", "code")) {
            int[] s = EvalSupport.score(questions, q -> results.get(q.id()), type, THRESHOLD);
            System.out.printf("  %-12s top-1 %d/%d   top-3 %d/%d%n", type, s[0], s[2], s[1], s[2]);
        }
        int[] oos = EvalSupport.score(questions, q -> results.get(q.id()), "out_of_scope", THRESHOLD);
        System.out.printf("  %-12s rejected %d/%d%n", "out_of_scope", oos[3], oos[2]);

        System.out.println("\nThreshold sweep (in-scope = direct + paraphrase + code):");
        System.out.println("  T    | top-1 | top-3 | out-of-scope rejected");
        for (double t : new double[]{0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50, 0.55, 0.60}) {
            int top1 = 0, top3 = 0, n = 0;
            for (String type : List.of("direct", "paraphrase", "code")) {
                int[] s = EvalSupport.score(questions, q -> results.get(q.id()), type, t);
                top1 += s[0];
                top3 += s[1];
                n += s[2];
            }
            int[] o = EvalSupport.score(questions, q -> results.get(q.id()), "out_of_scope", t);
            System.out.printf("  %.2f | %2d/%d | %2d/%d | %d/%d%n", t, top1, n, top3, n, o[3], o[2]);
        }
    }
}
