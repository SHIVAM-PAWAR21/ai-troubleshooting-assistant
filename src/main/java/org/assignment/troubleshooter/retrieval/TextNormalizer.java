package org.assignment.troubleshooter.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Turns free text into comparable tokens: lowercase, letters/digits only, no stop words, crude stemming. */
public final class TextNormalizer {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "am", "to", "of", "in", "on", "at",
            "for", "and", "or", "my", "our", "we", "i", "it", "its", "this", "that", "with", "from", "by",
            "not", "no", "do", "does", "did", "have", "has", "had", "when", "after", "get", "gets", "getting",
            "got", "keep", "keeps", "how", "what", "why", "up", "out", "me", "us", "you", "your", "they",
            "them", "there", "some", "any", "very", "just", "now", "again", "still", "so", "as", "if");

    private TextNormalizer() {
    }

    public static List<String> tokens(String text) {
        List<String> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        for (String raw : text.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim().split(" ")) {
            if (raw.isEmpty() || STOP_WORDS.contains(raw)) {
                continue;
            }
            result.add(stem(raw));
        }
        return result;
    }

    /** Very small stemmer (crashing -> crash, bounced -> bounc, errors -> error). Applied to both sides, so it only has to be consistent. */
    static String stem(String token) {
        if (token.length() > 5 && token.endsWith("ing")) {
            return token.substring(0, token.length() - 3);
        }
        if (token.length() > 4 && token.endsWith("ed")) {
            return token.substring(0, token.length() - 2);
        }
        if (token.length() > 3 && token.endsWith("s") && !token.endsWith("ss")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }
}
