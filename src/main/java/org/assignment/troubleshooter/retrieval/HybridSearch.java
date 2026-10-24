package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.ai.EmbeddingException;
import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The default mode: keyword search and semantic search, merged into one ranking.
 *
 * Why both: keywords are exact (error codes like 502 vs 503), embeddings understand different wording.
 * If the embedding model fails, it carries on with keywords only.
 */
@Component
public class HybridSearch implements SearchMode {

    private static final Logger log = LoggerFactory.getLogger(HybridSearch.class);

    private final Retriever keyword;
    private final Retriever semantic;
    private final RankFusion fusion;
    private final OutcomePolicy policy;

    public HybridSearch(KeywordRetriever keyword, SemanticRetriever semantic, RankFusion fusion, OutcomePolicy policy) {
        this.keyword = keyword;
        this.semantic = semantic;
        this.fusion = fusion;
        this.policy = policy;
    }

    @Override
    public RetrievalResult search(String query, List<KnowledgeEntry> kb) {
        List<ScoredEntry> keywordResults = keyword.search(query, kb);
        List<ScoredEntry> semanticResults;
        try {
            semanticResults = semantic.search(query, kb);
        } catch (EmbeddingException e) {
            log.warn("Semantic search failed, using keywords only: {}", e.getMessage());
            semanticResults = List.of();
        }
        return new RetrievalResult(
                policy.decideHybrid(keywordResults, semanticResults),
                fusion.fuse(keywordResults, semanticResults));
    }
}
