package org.assignment.troubleshooter.retrieval;

import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Keyword retrieval: scores every KB entry against the query using exact phrases, HTTP error codes
 * and shared words. No AI involved, so it is fast, free and easy to explain.
 *
 * Score = sum of:
 *   +5  per HTTP error code (4xx/5xx) in both the query and the entry
 *   +3  per entry keyword/phrase found in the query
 *   +2  per query word found in the entry title
 *   +1  per query word found in the entry symptoms (only if not already in the title)
 *   -4  if the query has an error code and the entry has codes, but none match (code conflict)
 * Entries scoring 0 or less are dropped.
 */
@Service
public class KeywordRetrievalService {

    static final double CODE_MATCH = 5;
    static final double KEYWORD_MATCH = 3;
    static final double TITLE_WORD = 2;
    static final double SYMPTOM_WORD = 1;
    static final double CODE_CONFLICT = 4;

    private static final Pattern ERROR_CODE = Pattern.compile("\\b([45]\\d\\d)\\b");

    public List<ScoredEntry> search(String query, List<KnowledgeEntry> kb) {
        List<String> queryTokens = TextNormalizer.tokens(query);
        Set<String> queryWords = new HashSet<>(queryTokens);
        Set<String> queryCodes = codesIn(query);

        return kb.stream()
                .map(entry -> new ScoredEntry(entry, score(entry, queryTokens, queryWords, queryCodes)))
                .filter(s -> s.score() > 0)
                .sorted(Comparator.comparingDouble(ScoredEntry::score).reversed()
                        .thenComparing(s -> s.entry().id()))
                .collect(Collectors.toList());
    }

    private double score(KnowledgeEntry entry, List<String> queryTokens, Set<String> queryWords, Set<String> queryCodes) {
        double score = 0;

        // 1. error codes
        Set<String> entryCodes = codesIn(entry.title() + " " + String.join(" ", entry.keywords()));
        if (!queryCodes.isEmpty() && !entryCodes.isEmpty()) {
            Set<String> matched = new HashSet<>(queryCodes);
            matched.retainAll(entryCodes);
            score += matched.isEmpty() ? -CODE_CONFLICT : CODE_MATCH * matched.size();
        }

        // 2. keywords and phrases
        for (String keyword : entry.keywords()) {
            List<String> kw = TextNormalizer.tokens(keyword);
            if (!kw.isEmpty() && Collections.indexOfSubList(queryTokens, kw) >= 0) {
                score += KEYWORD_MATCH;
            }
        }

        // 3. shared words with title and symptoms
        Set<String> titleWords = new HashSet<>(TextNormalizer.tokens(entry.title()));
        Set<String> symptomWords = new HashSet<>(TextNormalizer.tokens(entry.symptoms()));
        for (String word : queryWords) {
            if (titleWords.contains(word)) {
                score += TITLE_WORD;
            } else if (symptomWords.contains(word)) {
                score += SYMPTOM_WORD;
            }
        }
        return Math.max(0, score);
    }

    private static Set<String> codesIn(String text) {
        Set<String> codes = new HashSet<>();
        Matcher m = ERROR_CODE.matcher(text.toLowerCase());
        while (m.find()) {
            codes.add(m.group(1));
        }
        return codes;
    }
}
