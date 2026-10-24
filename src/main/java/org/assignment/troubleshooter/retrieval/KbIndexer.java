package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.ai.Embedder;
import org.assignment.troubleshooter.ai.EmbeddingException;
import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.assignment.troubleshooter.repository.KbRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Embeds every KB entry once and stores the vector in MySQL, so a question only needs one new embedding.
 * Entries that already have a vector from the current model are skipped.
 */
@Component
public class KbIndexer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KbIndexer.class);

    private final KbRepository repo;
    private final Embedder embedder;

    public KbIndexer(KbRepository repo, Embedder embedder) {
        this.repo = repo;
        this.embedder = embedder;
    }

    /** Returns how many entries were embedded in this run. */
    public int indexAll() {
        int embedded = 0;
        for (KnowledgeEntry e : repo.findAll()) {
            if (!e.hasEmbeddingFrom(embedder.modelId())) {
                repo.saveEmbedding(e.id(), embedder.embed(e.embeddingText()), embedder.modelId());
                embedded++;
            }
        }
        return embedded;
    }

    @Override
    public void run(String... args) {
        try {
            int n = indexAll();
            log.info("KB indexing done: {} entries embedded with {}", n, embedder.modelId());
        } catch (EmbeddingException e) {
            // The app stays up and can still answer with keyword search.
            log.warn("KB indexing failed, semantic search is unavailable: {}", e.getMessage());
        }
    }
}
