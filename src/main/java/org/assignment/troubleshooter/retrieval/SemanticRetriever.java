package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.ai.Embedder;
import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Semantic retrieval: embeds the question and ranks KB entries by cosine similarity to their stored vectors.
 * Entries without a vector from the current model are skipped, because vectors from different models can't be compared.
 */
@Component
public class SemanticRetriever implements Retriever {

    private final Embedder embedder;

    public SemanticRetriever(Embedder embedder) {
        this.embedder = embedder;
    }

    /** Returns every comparable entry, best first. No threshold is applied here. Throws EmbeddingException if the model fails. */
    @Override
    public List<ScoredEntry> search(String query, List<KnowledgeEntry> kb) {
        double[] q = embedder.embed(query);
        List<ScoredEntry> result = new ArrayList<>();
        for (KnowledgeEntry e : kb) {
            if (e.hasEmbeddingFrom(embedder.modelId())) {
                result.add(new ScoredEntry(e, VectorMath.cosine(q, e.embedding())));
            }
        }
        result.sort(Comparator.comparingDouble(ScoredEntry::score).reversed()
                .thenComparing(s -> s.entry().id()));
        return result;
    }
}
