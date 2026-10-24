package org.assignment.troubleshooter.model;

/** What the app decided about a question. Used by retrieval and sent to the browser as-is. */
public enum Status {
    /** A KB entry matches well enough to answer from it. */
    ANSWERED,
    /** Something may match, but not clearly (vague question, or several issues tie). Ask the user for more details. */
    NEED_MORE_INFORMATION,
    /** Nothing in the KB looks related. */
    INSUFFICIENT_KNOWLEDGE
}
