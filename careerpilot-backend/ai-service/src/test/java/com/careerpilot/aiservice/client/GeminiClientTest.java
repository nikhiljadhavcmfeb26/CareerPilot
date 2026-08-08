package com.careerpilot.aiservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression tests for the Gemini 401 bug.
 *
 * These run against a throwaway {@code com.sun.net.httpserver} instance rather
 * than MockRestServiceServer, because GeminiClient builds its own RestClient
 * internally - a real socket is the honest way to assert what actually goes on
 * the wire, which is the whole point here: the bug was that the API key went
 * into the query string instead of a header.
 */
class GeminiClientTest {

    private HttpServer server;
    private GeminiClient client;

    /** What the stub should reply with; set per test. */
    private int responseStatus = 200;
    private String responseBody = "{}";

    private final List<String> capturedUris = new ArrayList<>();
    private final List<String> capturedApiKeyHeaders = new ArrayList<>();
    private final List<String> capturedBodies = new ArrayList<>();
    private final AtomicInteger requestCount = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();

        client = new GeminiClient(new ObjectMapper());
        ReflectionTestUtils.setField(client, "apiKey", "AQ.TestAuthorizationKey");
        ReflectionTestUtils.setField(client, "model", "gemini-3.5-flash");
        ReflectionTestUtils.setField(client, "temperature", 0.4);
        ReflectionTestUtils.setField(client, "baseUrl",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        capturedUris.add(exchange.getRequestURI().toString());
        capturedApiKeyHeaders.add(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
        try (InputStream in = exchange.getRequestBody()) {
            capturedBodies.add(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatus, out.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
        }
    }

    private static String candidate(String text) {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + quote(text) + "}]}}]}";
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("THE FIX: the key is sent in the x-goog-api-key header, never as ?key=")
    void sendsApiKeyAsHeaderNotQueryParam() {
        responseBody = candidate("{\"score\":88}");

        client.generateJson("prompt", null);

        assertThat(capturedApiKeyHeaders).containsExactly("AQ.TestAuthorizationKey");
        assertThat(capturedUris).hasSize(1);
        assertThat(capturedUris.get(0))
                .as("the API key must never appear in the URL - that is what produced HTTP 401 "
                        + "ACCESS_TOKEN_TYPE_UNSUPPORTED for AQ. keys")
                .doesNotContain("key=")
                .doesNotContain("AQ.TestAuthorizationKey")
                .isEqualTo("/v1beta/models/gemini-3.5-flash:generateContent");
    }

    @Test
    @DisplayName("request body carries the prompt, the JSON response mime type and no PDF when none is given")
    void buildsTextOnlyRequest() {
        responseBody = candidate("{\"ok\":true}");

        client.generateJson("analyse this", null);

        String body = capturedBodies.get(0);
        assertThat(body).contains("\"role\":\"user\"");
        assertThat(body).contains("analyse this");
        assertThat(body).contains("\"responseMimeType\":\"application/json\"");
        assertThat(body).doesNotContain("inline_data");
    }

    @Test
    @DisplayName("a base64 PDF is attached as inline_data with the PDF mime type")
    void attachesPdfInline() {
        responseBody = candidate("{\"ok\":true}");

        client.generateJson("analyse this", "JVBERi0xLjQK");

        String body = capturedBodies.get(0);
        assertThat(body).contains("\"inline_data\"");
        assertThat(body).contains("\"mime_type\":\"application/pdf\"");
        assertThat(body).contains("JVBERi0xLjQK");
    }

    @Test
    @DisplayName("the model's JSON is returned unwrapped")
    void parsesModelJson() {
        responseBody = candidate("{\"score\":73,\"strengths\":[\"a\",\"b\"]}");

        JsonNode result = client.generateJson("prompt", null);

        assertThat(result.path("score").asInt()).isEqualTo(73);
        assertThat(result.path("strengths")).hasSize(2);
    }

    @Test
    @DisplayName("a markdown-fenced response is still parsed")
    void stripsCodeFences() {
        responseBody = candidate("```json\n{\"score\":50}\n```");

        JsonNode result = client.generateJson("prompt", null);

        assertThat(result.path("score").asInt()).isEqualTo(50);
    }

    @Test
    @DisplayName("401 produces an actionable message, not a bare HttpClientErrorException")
    void classifiesUnauthorized() {
        responseStatus = 401;
        responseBody = "{\"error\":{\"code\":401,\"message\":\"Request had invalid authentication credentials.\","
                + "\"status\":\"UNAUTHENTICATED\"}}";

        assertThatThrownBy(() -> client.generateJson("prompt", null))
                .isInstanceOf(GeminiClient.GeminiException.class)
                .hasMessageContaining("(401)")
                .hasMessageContaining("x-goog-api-key")
                .hasMessageContaining("UNAUTHENTICATED");
    }

    @Test
    @DisplayName("404 names the retired model - the other half of the original outage")
    void classifiesUnknownModel() {
        responseStatus = 404;
        responseBody = "{\"error\":{\"code\":404,\"message\":\"models/gemini-2.0-flash is not found\"}}";

        assertThatThrownBy(() -> client.generateJson("prompt", null))
                .isInstanceOf(GeminiClient.GeminiException.class)
                .hasMessageContaining("gemini-3.5-flash")
                .hasMessageContaining("(404)");
    }

    @Test
    @DisplayName("401 is not retried - retrying a bad key just wastes time")
    void doesNotRetryAuthFailures() {
        responseStatus = 401;
        responseBody = "{\"error\":{\"message\":\"bad key\"}}";

        assertThatThrownBy(() -> client.generateJson("prompt", null))
                .isInstanceOf(GeminiClient.GeminiException.class);

        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("429 is retried up to three times before giving up")
    void retriesRateLimits() {
        responseStatus = 429;
        responseBody = "{\"error\":{\"message\":\"quota exceeded\"}}";

        assertThatThrownBy(() -> client.generateJson("prompt", null))
                .isInstanceOf(GeminiClient.GeminiException.class)
                .hasMessageContaining("(429)");

        assertThat(requestCount.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("a safety block is reported as a block, not as an empty response")
    void reportsBlockReason() {
        responseBody = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";

        assertThatThrownBy(() -> client.generateJson("prompt", null))
                .isInstanceOf(GeminiClient.GeminiException.class)
                .hasMessageContaining("SAFETY");
    }

    @Test
    @DisplayName("a missing key fails before any network call is attempted")
    void failsFastWithoutKey() {
        ReflectionTestUtils.setField(client, "apiKey", "");

        assertThatThrownBy(() -> client.generateJson("prompt", null))
                .isInstanceOf(GeminiClient.GeminiException.class)
                .hasMessageContaining("GEMINI_API_KEY");

        assertThat(requestCount.get()).isZero();
    }
}
