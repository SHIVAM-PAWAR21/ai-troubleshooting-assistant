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

/** Phase 2: measures keyword-only retrieval on the labelled questions in eval-questions.json. */
@SpringBootTest
class KeywordEvalTest {

    /** Minimum keyword score to count as a match. Chosen from the sweep printed by this test. */
    static final double THRESHOLD = 3.0;

    @Autowired
    KbRepository repo;

    @Autowired
    KeywordRetriever keyword;

    @Test
    void keywordOnlyEvaluation() throws Exception {
        List<KnowledgeEntry> kb = repo.findAll();
        assertEquals(24, kb.size());

        List<EvalSupport.EvalQuestion> questions = EvalSupport.load();
        Map<String, List<ScoredEntry>> results = new HashMap<>();
        for (EvalSupport.EvalQuestion q : questions) {
            results.put(q.id(), keyword.search(q.question(), kb));
        }

        System.out.println("\n=== KEYWORD-ONLY EVALUATION (threshold " + THRESHOLD + ") ===");
        for (EvalSupport.EvalQuestion q : questions) {
            System.out.printf("%-3s %-12s expected=%-16s top3: %-32s | %s%n",
                    q.id(), q.type(), q.expected(), EvalSupport.top3(results.get(q.id())), q.question());
        }

        System.out.println("\nBy type at threshold " + THRESHOLD + ":");
        for (String type : List.of("direct", "paraphrase", "code")) {
            EvalSupport.Score s = EvalSupport.score(questions, q -> results.get(q.id()), type, THRESHOLD);
            System.out.printf("  %-12s top-1 %d/%d   top-3 %d/%d%n", type, s.top1(), s.count(), s.top3(), s.count());
        }
        EvalSupport.Score oos = EvalSupport.score(questions, q -> results.get(q.id()), "out_of_scope", THRESHOLD);
        System.out.printf("  %-12s rejected %d/%d%n", "out_of_scope", oos.rejected(), oos.count());

        System.out.println("\nThreshold sweep (in-scope = direct + paraphrase + code):");
        System.out.println("  T  | top-1 | top-3 | out-of-scope rejected");
        for (double t : new double[]{1, 2, 3, 4, 5, 6, 8, 10}) {
            int top1 = 0, top3 = 0, n = 0;
            for (String type : List.of("direct", "paraphrase", "code")) {
                EvalSupport.Score s = EvalSupport.score(questions, q -> results.get(q.id()), type, t);
                top1 += s.top1();
                top3 += s.top3();
                n += s.count();
            }
            EvalSupport.Score o = EvalSupport.score(questions, q -> results.get(q.id()), "out_of_scope", t);
            System.out.printf("  %-2.0f | %2d/%d | %2d/%d | %d/%d%n", t, top1, n, top3, n, o.rejected(), o.count());
        }

        // Regression guard: fails if a later change makes keyword retrieval clearly worse.
        int top3 = 0, n = 0;
        for (String type : List.of("direct", "paraphrase", "code")) {
            EvalSupport.Score s = EvalSupport.score(questions, q -> results.get(q.id()), type, THRESHOLD);
            top3 += s.top3();
            n += s.count();
        }
        assertTrue(top3 >= n - 2, "keyword top-3 dropped below " + (n - 2) + "/" + n);
        assertEquals(oos.count(), oos.rejected(), "an out-of-scope question matched a KB entry");
    }
}
