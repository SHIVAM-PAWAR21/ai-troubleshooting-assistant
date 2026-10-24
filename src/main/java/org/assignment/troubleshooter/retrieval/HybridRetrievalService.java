package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.ai.EmbeddingException;
import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hybrid retrieval: keyword search and semantic search, merged with reciprocal rank fusion (RRF).
 *
 * Why both: keywords are exact (error codes like 502 vs 503), embeddings understand different wording.
 * Why RRF: keyword scores (0-25) and cosine scores (0-1) are on different scales, so we add up
 * 1 / (60 + rank) from each list instead of adding the raw scores.
 *
 * The outcome is decided from the raw scores, not from the fused score, because the fused score
 * only says "better than the others", never "good enough".
 */
@Service
public class HybridRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(HybridRetrievalService.class);
    private static final double RRF_K = 60;

    private final KeywordRetrievalService keyword;
    private final SemanticRetrievalService semantic;
    private final Thresholds thresholds;

    @Autowired
    public HybridRetrievalService(KeywordRetrievalService keyword, SemanticRetrievalService semantic) {
        this(keyword, semantic, Thresholds.DEFAULT);
    }

    HybridRetrievalService(KeywordRetrievalService keyword, SemanticRetrievalService semantic, Thresholds thresholds) {
        this.keyword = keyword;
        this.semantic = semantic;
        this.thresholds = thresholds;
    }

    public HybridResult search(String query, List<KnowledgeEntry> kb) {
        List<ScoredEntry> keywordResults = keyword.search(query, kb);
        List<ScoredEntry> semanticResults;
        try {
            semanticResults = semantic.search(query, kb);
        } catch (EmbeddingException e) {
            log.warn("Semantic search failed, using keywords only: {}", e.getMessage());
            semanticResults = null;
        }
        return combine(keywordResults, semanticResults, thresholds);
    }

    /** Cosine cut-off for semantic-only mode. Tune it from the SemanticEvalTest sweep table. */
    public static final double SEMANTIC_ONLY_MIN = 0.40;

    public HybridResult searchSemanticOnly(String query, List<KnowledgeEntry> kb) {
        try {
            return decideSemanticOnly(semantic.search(query, kb));
        } catch (EmbeddingException e) {
            log.warn("Semantic-only search failed: {}", e.getMessage());
            return new HybridResult(RetrievalOutcome.INSUFFICIENT_KNOWLEDGE, List.of(), false);
        }
    }

    static HybridResult decideSemanticOnly(List<ScoredEntry> sem) {
        List<RankedEntry> ranked = new ArrayList<>();
        for (ScoredEntry s : sem) {
            ranked.add(new RankedEntry(s.entry(), s.score(), 0.0, s.score()));
        }
        boolean used = !sem.isEmpty();
        boolean match = used && sem.get(0).score() >= SEMANTIC_ONLY_MIN;
        return new HybridResult(
                match ? RetrievalOutcome.ANSWER : RetrievalOutcome.INSUFFICIENT_KNOWLEDGE, ranked, used);
    }


    /**
     * Merges the two ranked lists and decides the outcome. Pure function, so it can be tested without a database or model.
     *
     * @param keywordResults entries with a keyword score above 0, best first
     * @param semanticResults entries with a cosine score, best first; null if semantic search was not available
     */
    public static HybridResult combine(List<ScoredEntry> keywordResults, List<ScoredEntry> semanticResults, Thresholds t) {
        Map<String, KnowledgeEntry> entries = new HashMap<>();
        Map<String, Double> rrf = new HashMap<>();
        Map<String, Double> keywordScore = new HashMap<>();
        Map<String, Double> cosine = new HashMap<>();

        for (int i = 0; i < keywordResults.size(); i++) {
            ScoredEntry s = keywordResults.get(i);
            String id = s.entry().id();
            entries.put(id, s.entry());
            keywordScore.put(id, s.score());
            rrf.merge(id, 1.0 / (RRF_K + i + 1), Double::sum);
        }
        boolean semanticUsed = semanticResults != null && !semanticResults.isEmpty();
        if (semanticResults != null) {
            for (int i = 0; i < semanticResults.size(); i++) {
                ScoredEntry s = semanticResults.get(i);
                String id = s.entry().id();
                entries.put(id, s.entry());
                cosine.put(id, s.score());
                rrf.merge(id, 1.0 / (RRF_K + i + 1), Double::sum);
            }
        }

        List<RankedEntry> ranked = new ArrayList<>();
        for (Map.Entry<String, KnowledgeEntry> e : entries.entrySet()) {
            String id = e.getKey();
            ranked.add(new RankedEntry(e.getValue(), rrf.get(id), keywordScore.getOrDefault(id, 0.0), cosine.get(id)));
        }
        ranked.sort(Comparator.comparingDouble(RankedEntry::rrf).reversed()
                .thenComparing(Comparator.comparingDouble(RankedEntry::keywordScore).reversed())
                .thenComparing(r -> r.entry().id()));

        return new HybridResult(decide(keywordResults, semanticResults, semanticUsed, t), ranked, semanticUsed);
    }

    private static RetrievalOutcome decide(List<ScoredEntry> kw, List<ScoredEntry> sem, boolean semanticUsed, Thresholds t) {
        double topKeyword = kw.isEmpty() ? 0 : kw.get(0).score();
        double topCosine = semanticUsed ? sem.get(0).score() : Double.NEGATIVE_INFINITY;

        boolean weakKeyword = topKeyword < t.keywordMin();
        boolean weakSemantic = !semanticUsed || topCosine < t.cosineLow();
        boolean strongKeyword = topKeyword >= t.keywordStrong();
        boolean strongSemantic = semanticUsed && topCosine >= t.cosineHigh();

        // both methods point at the same entry, and neither is weak
        boolean agree = !kw.isEmpty() && semanticUsed
                && kw.get(0).entry().id().equals(sem.get(0).entry().id());
        // the keyword method cannot tell the top entries apart (e.g. "the API is not working")
        boolean tied = kw.size() >= 2 && kw.get(0).score() - kw.get(1).score() < 1.0;

        if (weakKeyword && weakSemantic) {
            return RetrievalOutcome.INSUFFICIENT_KNOWLEDGE;
        }
        boolean confident = strongKeyword || strongSemantic || (agree && !weakKeyword && !weakSemantic);
        if (confident && !(tied && !strongKeyword && !strongSemantic)) {
            return RetrievalOutcome.ANSWER;
        }
        return RetrievalOutcome.NEED_MORE_INFORMATION;
    }
}
