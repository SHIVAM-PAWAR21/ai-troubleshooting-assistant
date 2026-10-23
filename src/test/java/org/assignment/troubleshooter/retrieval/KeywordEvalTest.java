package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.assignment.troubleshooter.repository.KbRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phase 2: measures keyword-only retrieval on the labelled questions in eval-questions.json. */
@SpringBootTest
class KeywordEvalTest {

    record EvalQuestion(String id, String type, String question, List<String> expected) {
    }

    /** Minimum keyword score to count as a match. Chosen from the sweep printed by this test. */
    static final double THRESHOLD = 3.0;

    @Autowired
    KbRepository repo;

    @Autowired
    KeywordRetrievalService keyword;

    @Test
    void keywordOnlyEvaluation() throws Exception {
        List<KnowledgeEntry> kb = repo.findAll();
        assertEquals(15, kb.size());

        List<EvalQuestion> questions;
        try (InputStream in = getClass().getResourceAsStream("/eval-questions.json")) {
            questions = JsonMapper.builder().build().readValue(in, new TypeReference<List<EvalQuestion>>() {});
        }

        System.out.println("\n=== KEYWORD-ONLY EVALUATION (threshold " + THRESHOLD + ") ===");
        for (EvalQuestion q : questions) {
            List<ScoredEntry> r = keyword.search(q.question(), kb);
            StringBuilder top = new StringBuilder();
            for (int i = 0; i < Math.min(3, r.size()); i++) {
                top.append(r.get(i).entry().id()).append("=").append(Math.round(r.get(i).score())).append(" ");
            }
            System.out.printf("%-3s %-12s expected=%-16s top3: %-26s | %s%n",
                    q.id(), q.type(), q.expected(), top.toString().trim(), q.question());
        }

        System.out.println("\nBy type at threshold " + THRESHOLD + ":");
        for (String type : List.of("direct", "paraphrase", "code")) {
            int[] s = score(questions, kb, type, THRESHOLD);
            System.out.printf("  %-12s top-1 %d/%d   top-3 %d/%d%n", type, s[0], s[2], s[1], s[2]);
        }
        int[] oos = score(questions, kb, "out_of_scope", THRESHOLD);
        System.out.printf("  %-12s rejected %d/%d%n", "out_of_scope", oos[3], oos[2]);

        System.out.println("\nThreshold sweep (in-scope = direct + paraphrase + code):");
        System.out.println("  T  | top-1 | top-3 | out-of-scope rejected");
        for (double t : new double[]{1, 2, 3, 4, 5, 6, 8, 10}) {
            int top1 = 0, top3 = 0, n = 0;
            for (String type : List.of("direct", "paraphrase", "code")) {
                int[] s = score(questions, kb, type, t);
                top1 += s[0];
                top3 += s[1];
                n += s[2];
            }
            int[] o = score(questions, kb, "out_of_scope", t);
            System.out.printf("  %-2.0f | %2d/%d | %2d/%d | %d/%d%n", t, top1, n, top3, n, o[3], o[2]);
        }

        // Regression guard: fails if a later change makes keyword retrieval clearly worse.
        int top3 = 0, n = 0;
        for (String type : List.of("direct", "paraphrase", "code")) {
            int[] s = score(questions, kb, type, THRESHOLD);
            top3 += s[1];
            n += s[2];
        }
        assertTrue(top3 >= n - 2, "keyword top-3 dropped below " + (n - 2) + "/" + n);
        assertEquals(oos[2], oos[3], "an out-of-scope question matched a KB entry");
    }

    /** Returns {top1 hits, top3 hits, question count, rejected (no entry at or above threshold)} for one question type. */
    private int[] score(List<EvalQuestion> all, List<KnowledgeEntry> kb, String type, double threshold) {
        int top1 = 0, top3 = 0, count = 0, rejected = 0;
        for (EvalQuestion q : all) {
            if (!q.type().equals(type)) {
                continue;
            }
            count++;
            List<ScoredEntry> r = keyword.search(q.question(), kb);
            boolean matched = !r.isEmpty() && r.get(0).score() >= threshold;
            if (!matched) {
                rejected++;
            }
            if (matched && q.expected().contains(r.get(0).entry().id())) {
                top1++;
            }
            for (int i = 0; i < Math.min(3, r.size()); i++) {
                if (r.get(i).score() >= threshold && q.expected().contains(r.get(i).entry().id())) {
                    top3++;
                    break;
                }
            }
        }
        return new int[]{top1, top3, count, rejected};
    }
}
