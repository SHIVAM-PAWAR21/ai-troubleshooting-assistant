package org.assignment.troubleshooter.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Calls the Groq chat API (OpenAI format) with plain HTTP. Any problem becomes an LlmException. */
@Component
public class GroqLlmClient implements LlmClient {

    private final String apiKey;
    private final String model;
    private final String url;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final JsonMapper mapper = JsonMapper.builder().build();

    public GroqLlmClient(@Value("${assistant.llm.api-key:}") String apiKey,
                         @Value("${assistant.llm.model}") String model,
                         @Value("${assistant.llm.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.url = baseUrl + "/chat/completions";
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey.isBlank()) {
            throw new LlmException("GROQ_API_KEY is not set");
        }
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.1,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt))));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new LlmException("Groq returned HTTP " + response.statusCode());
            }
            ChatResponse reply = mapper.readValue(response.body(), ChatResponse.class);
            if (reply.choices() == null || reply.choices().isEmpty() || reply.choices().get(0).message() == null) {
                throw new LlmException("Groq returned no message");
            }
            return reply.choices().get(0).message().content();
        } catch (IOException | JacksonException e) {
            throw new LlmException("Could not call Groq: " + e, e); // e.g. ConnectException has no message of its own
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Call to Groq was interrupted", e);
        }
    }

    // Only the parts of Groq's reply that we use: choices[0].message.content
    // Jackson 3 ignores the fields we don't list
    public record ChatResponse(List<Choice> choices) {
    }

    public record Choice(Message message) {
    }

    public record Message(String content) {
    }
}
