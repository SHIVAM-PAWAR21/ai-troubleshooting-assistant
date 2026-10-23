package org.assignment.troubleshooter.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Calls the Groq chat API (OpenAI format) with plain HTTP. Any problem becomes an exception. */
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
            throw new IllegalStateException("GROQ_API_KEY is not set");
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
                throw new IllegalStateException("Groq returned HTTP " + response.statusCode());
            }
            return mapper.readValue(response.body(), ChatResponse.class).choices().get(0).message().content();
        } catch (IOException e) {
            throw new IllegalStateException("Could not call Groq: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Call to Groq was interrupted", e);
        }
    }

    // Only the parts of Groq's reply that we use: choices[0].message.content
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String content) {
    }
}
