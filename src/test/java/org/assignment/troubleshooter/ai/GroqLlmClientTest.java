package org.assignment.troubleshooter.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the client against a fake chat API on this machine. No real key or internet needed. */
class GroqLlmClientTest {

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();

    private String startFakeApi(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requests.incrementAndGet();
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsTheRequestAndReturnsTheReply() throws Exception {
        String url = startFakeApi(200, "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"hello\"}}]}");
        GroqLlmClient client = new GroqLlmClient("secret-key", "test-model", url);

        assertEquals("hello", client.complete("be helpful", "what is 2+2"));
        assertEquals("Bearer secret-key", lastAuth.get());
        assertTrue(lastBody.get().contains("\"model\":\"test-model\""));
        assertTrue(lastBody.get().contains("be helpful") && lastBody.get().contains("what is 2+2"));
    }

    @Test
    void errorStatusThrows() throws Exception {
        String url = startFakeApi(401, "{\"error\":\"invalid key\"}");
        GroqLlmClient client = new GroqLlmClient("wrong", "test-model", url);

        LlmException e = assertThrows(LlmException.class, () -> client.complete("s", "u"));
        assertTrue(e.getMessage().contains("401"));
    }

    @Test
    void missingKeyThrowsWithoutCallingTheApi() throws Exception {
        String url = startFakeApi(200, "{}");
        GroqLlmClient client = new GroqLlmClient("", "test-model", url);

        assertThrows(LlmException.class, () -> client.complete("s", "u"));
        assertEquals(0, requests.get());
    }
}
