package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.KnowledgeEntry;

import java.util.List;

/** One way of finding and judging KB entries for a question: hybrid, or semantic-only. */
public interface SearchMode {

    RetrievalResult search(String query, List<KnowledgeEntry> kb);

    /** What to tell the user when this mode finds nothing. */
    default String noMatchMessage(RetrievalResult result) {
        return "I could not find anything related to this in the knowledge base. Please share more details.";
    }
}
