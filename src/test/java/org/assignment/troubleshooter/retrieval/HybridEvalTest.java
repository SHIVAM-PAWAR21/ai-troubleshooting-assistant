package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.assignment.troubleshooter.model.Status;
import org.assignment.troubleshooter.repository.KbRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Phase 4: compares keyword-only, semantic-only and hybrid retrieval, and prints data for choosing the thresholds. */
@SpringBootTest
class HybridEvalTest {

    private static final List<String> IN_SCOPE = List.of("direct", "paraphrase", "code");

    @Autowired
    KbRepository repo;

    @Autowired
    KbIndexer indexer;

    @Autowired
    KeywordRetriever keyword;

    @Autowired
    SemanticRetriever semantic;

    @Test
    void hybridEvaluation() throws Exception {
        indexer.indexAll();
        List<KnowledgeEntry> kb = repo.findAll();
        assertEquals(24, kb.size());
        List<EvalSupport.EvalQuestion> qs = EvalSupport.load();

        Map<String, List<ScoredEntry>> kw = new HashMap<>();
        Map<String, List<ScoredEntry>> sem = new HashMap<>();
        for (EvalSupport.EvalQuestion q : qs) {
            kw.put(q.id(), keyword.search(q.question(), kb));
            sem.put(q.id(), semantic.search(q.question(), kb));
        }

        // 1. Ranking quality only: is the right entry first / in the top 3? No thresholds involved.
        System.out.println("\n=== 1. RANKING QUALITY (in-scope questions, no thresholds) ===");
        System.out.println("method          | type        | top-1 | top-3");
        printRanking("keyword-only", qs, q -> ids(kw.get(q.id())));
        printRanking("semantic-only", qs, q -> ids(sem.get(q.id())));
        printRanking("hybrid (RRF)", qs, q -> combine(kw.get(q.id()), sem.get(q.id()), Thresholds.DEFAULT)
                .ranked().stream().map(r -> r.entry().id()).collect(Collectors.toList()));

        // 2. Outcome per question with the default thresholds.
        Thresholds d = Thresholds.DEFAULT;
        System.out.println("\n=== 2. OUTCOMES at " + d + " ===");
        for (EvalSupport.EvalQuestion q : qs) {
            RetrievalResult r = combine(kw.get(q.id()), sem.get(q.id()), d);
            RankedEntry top = r.ranked().get(0);
            System.out.printf("%-3s %-12s %-22s %-7s expected=%-16s kw=%-3.0f cos=%-5s | %s%n",
                    q.id(), q.type(), r.outcome(), correct(q, r) ? "ok" : "WRONG", q.expected(),
                    kw.get(q.id()).isEmpty() ? 0 : kw.get(q.id()).get(0).score(),
                    sem.get(q.id()).isEmpty() ? "-" : String.format("%.2f", sem.get(q.id()).get(0).score()),
                    q.question() + "  -> " + top.entry().id());
        }

        // 3. Grid over the two cosine thresholds.
        System.out.println("\n=== 3. THRESHOLD GRID (keywordMin=" + d.keywordMin() + ", keywordStrong=" + d.keywordStrong() + ") ===");
        System.out.println("cosLow | cosHigh | correct | FALSE ANSWERS | missed answers");
        for (double low : new double[]{0.25, 0.30, 0.35, 0.40, 0.45}) {
            for (double high : new double[]{0.45, 0.50, 0.55, 0.60}) {
                if (low >= high) {
                    continue;
                }
                Thresholds t = new Thresholds(d.keywordMin(), d.keywordStrong(), low, high, d.semanticOnlyMin());
                int correct = 0, falseAnswers = 0, missed = 0;
                for (EvalSupport.EvalQuestion q : qs) {
                    RetrievalResult r = combine(kw.get(q.id()), sem.get(q.id()), t);
                    if (correct(q, r)) {
                        correct++;
                    }
                    if (r.outcome() == Status.ANSWERED && !rightEntry(q, r)) {
                        falseAnswers++;
                    }
                    if (IN_SCOPE.contains(q.type()) && r.outcome() != Status.ANSWERED) {
                        missed++;
                    }
                }
                System.out.printf("%.2f   | %.2f    | %2d/%d   | %d             | %d%n",
                        low, high, correct, qs.size(), falseAnswers, missed);
            }
        }
        System.out.println("\nFALSE ANSWERS = answered with the wrong entry, or answered an out-of-scope/vague question. This is the dangerous error.");
    }

    /** Same steps as HybridSearch, with the thresholds under test instead of the defaults. */
    private static RetrievalResult combine(List<ScoredEntry> kw, List<ScoredEntry> sem, Thresholds t) {
        return new RetrievalResult(new OutcomePolicy(t).decideHybrid(kw, sem), new RankFusion().fuse(kw, sem));
    }

    private static List<String> ids(List<ScoredEntry> list) {
        return list.stream().map(s -> s.entry().id()).collect(Collectors.toList());
    }

    private static void printRanking(String method, List<EvalSupport.EvalQuestion> qs,
                                     Function<EvalSupport.EvalQuestion, List<String>> order) {
        int allTop1 = 0, allTop3 = 0, allN = 0;
        for (String type : IN_SCOPE) {
            int top1 = 0, top3 = 0, n = 0;
            for (EvalSupport.EvalQuestion q : qs) {
                if (!q.type().equals(type)) {
                    continue;
                }
                n++;
                List<String> ranked = order.apply(q);
                if (!ranked.isEmpty() && q.expected().contains(ranked.get(0))) {
                    top1++;
                }
                if (ranked.stream().limit(3).anyMatch(q.expected()::contains)) {
                    top3++;
                }
            }
            System.out.printf("%-15s | %-11s | %d/%d   | %d/%d%n", method, type, top1, n, top3, n);
            allTop1 += top1;
            allTop3 += top3;
            allN += n;
        }
        System.out.printf("%-15s | %-11s | %d/%d | %d/%d%n", method, "ALL", allTop1, allN, allTop3, allN);
    }

    private static boolean rightEntry(EvalSupport.EvalQuestion q, RetrievalResult r) {
        return IN_SCOPE.contains(q.type()) && q.expected().contains(r.ranked().get(0).entry().id());
    }

    /** in-scope: answered with an expected entry. vague: anything except ANSWER. out of scope: INSUFFICIENT_KNOWLEDGE. */
    private static boolean correct(EvalSupport.EvalQuestion q, RetrievalResult r) {
        return switch (q.type()) {
            case "vague" -> r.outcome() != Status.ANSWERED;
            case "out_of_scope" -> r.outcome() == Status.INSUFFICIENT_KNOWLEDGE;
            default -> r.outcome() == Status.ANSWERED && rightEntry(q, r);
        };
    }
}
