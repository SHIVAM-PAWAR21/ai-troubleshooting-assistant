package org.assignment.troubleshooter.service;

import org.assignment.troubleshooter.ai.LlmClient;
import org.assignment.troubleshooter.ai.LlmException;
import org.assignment.troubleshooter.dto.TroubleshootResponse;
import org.assignment.troubleshooter.dto.TroubleshootResponse.Match;
import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.assignment.troubleshooter.repository.KbRepository;
import org.assignment.troubleshooter.retrieval.HybridSearch;
import org.assignment.troubleshooter.retrieval.RetrievalResult;
import org.assignment.troubleshooter.retrieval.SearchMode;
import org.assignment.troubleshooter.retrieval.SemanticOnlySearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The whole flow in one place:
 * 1. search the KB with the chosen mode, which also decides: answer, ask for more, or "not in the KB",
 * 2. only when the match is clear, ask the chat model to explain the entry,
 * 3. if the chat model fails, still answer with the entry's own steps.
 */
@Service
public class TroubleshootingService {

    private static final Logger log = LoggerFactory.getLogger(TroubleshootingService.class);

    private final KbRepository repo;
    private final SearchMode hybridSearch;
    private final SearchMode semanticOnlySearch;
    private final PromptBuilder prompts;
    private final LlmClient llm;

    public TroubleshootingService(KbRepository repo, HybridSearch hybridSearch, SemanticOnlySearch semanticOnlySearch,
                                  PromptBuilder prompts, LlmClient llm) {
        this.repo = repo;
        this.hybridSearch = hybridSearch;
        this.semanticOnlySearch = semanticOnlySearch;
        this.prompts = prompts;
        this.llm = llm;
    }

    public TroubleshootResponse troubleshoot(String query, boolean semanticOnly) {
        SearchMode mode = semanticOnly ? semanticOnlySearch : hybridSearch;
        RetrievalResult result = mode.search(query, repo.findAll());
        List<Match> matches = matches(result);

        return switch (result.outcome()) {
            case ANSWERED -> answer(query, result.ranked().get(0).entry(), matches);
            case NEED_MORE_INFORMATION -> TroubleshootResponse.needMoreInfo(
                    "I am not sure which issue this is. The entries below may be related, but I need more details.",
                    result.ranked().stream().limit(2).map(r -> r.entry().label()).toList(),
                    matches);
            case INSUFFICIENT_KNOWLEDGE -> TroubleshootResponse.notFound(mode.noMatchMessage(result), matches);
        };
    }

    private TroubleshootResponse answer(String query, KnowledgeEntry entry, List<Match> matches) {
        try {
            LlmReply reply = LlmReply.parse(llm.complete(prompts.systemPrompt(), prompts.userPrompt(query, entry)));
            if (!reply.covered()) {
                return TroubleshootResponse.notFound(
                        "The closest knowledge-base entry does not fit this issue. Please share more details.", matches);
            }
            return TroubleshootResponse.answered(reply.summary(), reply.steps(), entry, matches);
        } catch (LlmException e) {
            // Groq is down, the key is wrong, or the reply was unusable: still help, using the KB's own steps.
            log.warn("Chat model failed, using the knowledge-base steps instead: {}", e.getMessage());
            return TroubleshootResponse.answered(
                    "The AI summary is unavailable, so these are the steps stored in the knowledge base for: "
                            + entry.title() + ".",
                    entry.diagnosticSteps(), entry, matches);
        }
    }

    /** The three closest entries and their scores, so the page can show how the decision was made. */
    private static List<Match> matches(RetrievalResult result) {
        return result.ranked().stream().limit(3)
                .map(r -> new Match(r.entry().id(), r.entry().title(), r.keywordScore(),
                        r.cosine() == null ? null : Math.round(r.cosine() * 100) / 100.0))
                .toList();
    }
}
