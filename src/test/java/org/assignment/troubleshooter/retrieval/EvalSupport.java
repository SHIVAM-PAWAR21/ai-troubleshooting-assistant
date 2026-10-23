package org.assignment.troubleshooter.retrieval;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;

/** Shared helpers for the evaluation tests: loads eval-questions.json and counts hits. */
final class EvalSupport {

    record EvalQuestion(String id, String type, String question, List<String> expected) {
    }

    private EvalSupport() {
    }

    static List<EvalQuestion> load() throws IOException {
        try (InputStream in = EvalSupport.class.getResourceAsStream("/eval-questions.json")) {
            return JsonMapper.builder().build().readValue(in, new TypeReference<List<EvalQuestion>>() {});
        }
    }

    /** Returns {top1 hits, top3 hits, question count, rejected} for one question type at one threshold. */
    static int[] score(List<EvalQuestion> all, Function<EvalQuestion, List<ScoredEntry>> results,
                       String type, double threshold) {
        int top1 = 0, top3 = 0, count = 0, rejected = 0;
        for (EvalQuestion q : all) {
            if (!q.type().equals(type)) {
                continue;
            }
            count++;
            List<ScoredEntry> r = results.apply(q);
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

    static String top3(List<ScoredEntry> r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, r.size()); i++) {
            sb.append(r.get(i).entry().id()).append("=").append(String.format("%.2f", r.get(i).score())).append(" ");
        }
        return sb.toString().trim();
    }
}
