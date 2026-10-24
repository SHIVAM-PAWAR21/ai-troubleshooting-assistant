package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.KnowledgeEntry;

import java.util.List;

/** One way of scoring KB entries against a question (by keywords, or by meaning). */
public interface Retriever {

    /** Returns the scored entries, best first. */
    List<ScoredEntry> search(String query, List<KnowledgeEntry> kb);
}
