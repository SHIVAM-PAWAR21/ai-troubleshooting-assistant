package org.assignment.troubleshooter.ai;

/** Sends one prompt to a chat model and returns its text reply. Throws an exception if anything goes wrong. */
public interface LlmClient {

    String complete(String systemPrompt, String userPrompt);
}
