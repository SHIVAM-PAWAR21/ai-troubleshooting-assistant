package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.ai.EmbeddingException;
import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Matches by meaning only, with no keyword matching. Useful when the wording misleads the keyword search.
 * It never asks for more information: an entry is either close enough, or nothing is returned.
 */
@Component
public class SemanticOnlySearch implements SearchMode {

    private static final Logger log = LoggerFactory.getLogger(SemanticOnlySearch.class);

    private final Retriever semantic;
    private final OutcomePolicy policy;

    public SemanticOnlySearch(SemanticRetriever semantic, OutcomePolicy policy) {
        this.semantic = semantic;
        this.policy = policy;
    }

    @Override
    public RetrievalResult search(String query, List<KnowledgeEntry> kb) {
        List<ScoredEntry> semanticResults;
        try {
            semanticResults = semantic.search(query, kb);
        } catch (EmbeddingException e) {
            log.warn("Semantic-only search failed: {}", e.getMessage());
            semanticResults = List.of();
        }
        List<RankedEntry> ranked = semanticResults.stream()
                .map(s -> new RankedEntry(s.entry(), s.score(), 0, s.score()))
                .toList();
        return new RetrievalResult(policy.decideSemanticOnly(semanticResults), ranked);
    }

    /** An empty ranking means the model failed or no entry has a vector, so there was nothing to compare. */
    @Override
    public String noMatchMessage(RetrievalResult result) {
        return result.ranked().isEmpty()
                ? "Semantic-only mode: semantic search is not available right now, so nothing was returned."
                : "Semantic-only mode: no knowledge-base entry is close enough in meaning to this issue.";
    }
}
