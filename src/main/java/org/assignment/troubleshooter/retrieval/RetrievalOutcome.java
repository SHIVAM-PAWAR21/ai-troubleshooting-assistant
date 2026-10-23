package org.assignment.troubleshooter.retrieval;

/** What the retrieval step decided about a question. */
public enum RetrievalOutcome {
    /** A KB entry matches well enough to answer from it. */
    ANSWER,
    /** Something may match, but not clearly (vague question, or several issues tie). Ask the user for more details. */
    NEED_MORE_INFORMATION,
    /** Nothing in the KB looks related. */
    INSUFFICIENT_KNOWLEDGE
}
