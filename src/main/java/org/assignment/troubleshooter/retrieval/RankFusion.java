package org.assignment.troubleshooter.retrieval;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges the keyword and semantic rankings with reciprocal rank fusion (RRF).
 *
 * Keyword scores (0-25) and cosine scores (0-1) are on different scales, so instead of adding the raw scores
 * each list gives an entry 1 / (60 + rank). An entry found by both lists gets both shares and moves up.
 */
@Component
public class RankFusion {

    private static final double K = 60;

    /**
     * @param keywordResults  best first
     * @param semanticResults best first; empty if semantic search was not available
     */
    public List<RankedEntry> fuse(List<ScoredEntry> keywordResults, List<ScoredEntry> semanticResults) {
        Map<String, RankedEntry> byId = new HashMap<>();
        for (int i = 0; i < keywordResults.size(); i++) {
            ScoredEntry s = keywordResults.get(i);
            byId.merge(s.entry().id(), new RankedEntry(s.entry(), share(i), s.score(), null), RankFusion::addUp);
        }
        for (int i = 0; i < semanticResults.size(); i++) {
            ScoredEntry s = semanticResults.get(i);
            byId.merge(s.entry().id(), new RankedEntry(s.entry(), share(i), 0, s.score()), RankFusion::addUp);
        }

        List<RankedEntry> ranked = new ArrayList<>(byId.values());
        ranked.sort(Comparator.comparingDouble(RankedEntry::rrf).reversed()
                .thenComparing(Comparator.comparingDouble(RankedEntry::keywordScore).reversed())
                .thenComparing(r -> r.entry().id()));
        return ranked;
    }

    /** The RRF share for the entry at this position (0 = first). */
    private static double share(int position) {
        return 1.0 / (K + position + 1);
    }

    /** Same entry seen in both lists: add the shares and keep the keyword score and cosine from each side. */
    private static RankedEntry addUp(RankedEntry fromKeywords, RankedEntry fromSemantic) {
        return new RankedEntry(fromKeywords.entry(), fromKeywords.rrf() + fromSemantic.rrf(),
                fromKeywords.keywordScore(), fromSemantic.cosine());
    }
}
