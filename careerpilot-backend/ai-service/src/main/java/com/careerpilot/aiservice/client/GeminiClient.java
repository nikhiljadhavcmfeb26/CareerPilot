package com.careerpilot.aiservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

/**
 * Talks to the Gemini API's generateContent REST endpoint directly.
 *
 * <h2>Why this file was rewritten (the HTTP 401 bug)</h2>
 *
 * The previous implementation authenticated by appending {@code ?key=API_KEY}
 * to the URL and targeted the model {@code gemini-2.0-flash}. Both of those
 * are now broken, for two independent reasons:
 *
 * <ol>
 *   <li><b>Authentication.</b> Google AI Studio no longer issues the old
 *       "Standard" API keys ({@code AIza...}). Every new key is an
 *       <i>authorization key</i> ({@code AQ.Ab...}) bound to a Cloud service
 *       account. Authorization keys must be presented in the
 *       {@code x-goog-api-key} request header; passing one in the {@code ?key=}
 *       query parameter is rejected with
 *       <b>HTTP 401 ACCESS_TOKEN_TYPE_UNSUPPORTED</b>. That is exactly the
 *       exception this project was seeing
 *       ({@code HttpClientErrorException$Unauthorized} out of
 *       {@code GeminiClient.generateJson()}). The header form works for BOTH
 *       key generations, so it is used unconditionally.</li>
 *   <li><b>Model.</b> {@code gemini-2.0-flash} reached its shutdown date on
 *       1 June 2026 and the endpoint no longer exists. Google's named
 *       replacement is {@code gemini-3.5-flash}, which is the new default
 *       here (still overridable with {@code GEMINI_MODEL}).</li>
 * </ol>
 *
 * Reference: <a href="https://ai.google.dev/gemini-api/docs/api-key">Using
 * Gemini API keys</a> and
 * <a href="https://ai.google.dev/gemini-api/docs/deprecations">Gemini
 * deprecations</a>.
 *
 * <h2>Behaviour</h2>
 *
 * Resumes are sent as inline multimodal PDF data alongside the text prompt -
 * Gemini's document understanding handles the PDF natively, so no PDF
 * text-extraction library is needed.
 *
 * Errors are classified rather than swallowed: a bad key, a retired model and
 * a rate limit produce three different, actionable messages, and transient
 * failures (429 / 5xx / socket errors) are retried with backoff.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    /**
     * The header Google's REST API expects. Works with both the legacy
     * "AIza..." standard keys and the current "AQ.Ab..." authorization keys.
     * The {@code ?key=} query parameter only ever worked for the former and is
     * deliberately NOT used anywhere in this class.
     */
    private static final String API_KEY_HEADER = "x-goog-api-key";

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BASE_DELAY_MS = 1_500L;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.5-flash}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${gemini.temperature:0.4}")
    private double temperature;

    public GeminiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        // Screening a long resume against a long job description can genuinely
        // take the better part of a minute on a flash model.
        requestFactory.setReadTimeout(60_000);

        // No status handlers are registered on purpose. Every call goes through
        // exchange(), which hands us the raw ClientHttpResponse and bypasses
        // RestClient's default error handling entirely. That default is what
        // made the original bug so hard to diagnose: it threw
        // HttpClientErrorException$Unauthorized with the response body already
        // discarded, so the actual reason Google sent back
        // ("ACCESS_TOKEN_TYPE_UNSUPPORTED") never reached the log. We read the
        // body ourselves and classify on it - see describeFailure().
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * Fails loudly at startup rather than at the first user request. An AI
     * service with no key is misconfigured, not merely degraded.
     */
    @PostConstruct
    void verifyConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("gemini.api-key is not set. Every AI endpoint will fail until GEMINI_API_KEY "
                    + "is exported (get a key at https://aistudio.google.com/apikey).");
            return;
        }
        if (apiKey.startsWith("AIza")) {
            log.warn("A legacy 'Standard' Gemini API key (AIza...) is configured. Google already "
                    + "rejects unrestricted standard keys and retires ALL standard keys in "
                    + "September 2026 - create an authorization key (AQ....) in AI Studio and set "
                    + "GEMINI_API_KEY to it.");
        }
        log.info("Gemini client ready: model={}, baseUrl={}, auth via '{}' header.",
                model, baseUrl, API_KEY_HEADER);
    }

    /** Raised for every Gemini failure. Message is safe to log; callers decide what to show a user. */
    public static class GeminiException extends RuntimeException {
        public GeminiException(String message, Throwable cause) {
            super(message, cause);
        }

        public GeminiException(String message) {
            super(message);
        }
    }

    /**
     * @param promptText instructions for the model, including a description of
     *                   the exact JSON shape expected back
     * @param base64Pdf  resume file content, base64-encoded; null if this
     *                   particular call has no document to attach
     * @return the model's response, already parsed as JSON (not the raw Gemini
     * envelope - just the content the model generated)
     */
    public JsonNode generateJson(String promptText, String base64Pdf) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiException("Gemini is not configured: gemini.api-key (GEMINI_API_KEY) is empty.");
        }

        String url = baseUrl + "/models/" + model + ":generateContent";
        String requestBody = buildRequestBody(promptText, base64Pdf);

        Response response = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            response = exchange(url, requestBody);

            if (response.status().is2xxSuccessful()) {
                return parseModelJson(response.body());
            }
            if (!isRetryable(response.status().value()) || attempt == MAX_ATTEMPTS) {
                break;
            }

            long delay = RETRY_BASE_DELAY_MS * attempt;
            log.warn("Gemini returned HTTP {} (attempt {}/{}), retrying in {} ms",
                    response.status().value(), attempt, MAX_ATTEMPTS, delay);
            sleep(delay);
        }

        throw describeFailure(response);
    }

    /**
     * Cheap liveness probe used by the admin AI-settings screen: performs one
     * tiny real call so the operator can tell a broken key from a broken
     * feature without submitting a resume.
     */
    public String healthCheck() {
        if (apiKey == null || apiKey.isBlank()) {
            return "NOT_CONFIGURED: gemini.api-key (GEMINI_API_KEY) is empty.";
        }
        try {
            JsonNode result = generateJson(
                    "Respond with ONLY this JSON object and nothing else: {\"ok\": true}", null);
            return result.path("ok").asBoolean(false) ? "OK" : "UNEXPECTED_RESPONSE: " + result;
        } catch (GeminiException ex) {
            return "FAILED: " + ex.getMessage();
        }
    }

    // ---- request / response plumbing ----

    private String buildRequestBody(String promptText, String base64Pdf) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode content = requestBody.putArray("contents").addObject();
        // "role" is optional for single-turn calls but explicit is better -
        // omitting it is a common source of 400s once history is added.
        content.put("role", "user");
        var parts = content.putArray("parts");
        parts.addObject().put("text", promptText);

        if (base64Pdf != null && !base64Pdf.isBlank()) {
            ObjectNode inlineData = parts.addObject().putObject("inline_data");
            inlineData.put("mime_type", "application/pdf");
            inlineData.put("data", base64Pdf);
        }

        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", temperature);

        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (Exception ex) {
            throw new GeminiException("Could not serialise the Gemini request body", ex);
        }
    }

    private Response exchange(String url, String requestBody) {
        try {
            return restClient.post()
                    .uri(url)
                    .header(API_KEY_HEADER, apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .exchange((request, response) -> new Response(
                            response.getStatusCode(),
                            // Body is fully consumed inside this lambda, so it is
                            // safe (and correct) to let RestClient close the
                            // response for us.
                            new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)), true);
        } catch (ResourceAccessException ex) {
            // Connect/read timeout or DNS failure - no HTTP status to classify.
            throw new GeminiException("Could not reach the Gemini API (" + ex.getMessage() + ")", ex);
        } catch (GeminiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GeminiException("Gemini API call failed", ex);
        }
    }

    private static boolean isRetryable(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    /**
     * Turns a non-2xx Gemini response into an exception whose message names the
     * actual cause. Getting this wrong is what made the original bug expensive:
     * a 401 caused by the key being in the query string looked identical to a
     * 401 caused by an invalid key.
     */
    private GeminiException describeFailure(Response response) {
        if (response == null) {
            return new GeminiException("Gemini API call failed with no response.");
        }

        int status = response.status().value();
        String apiMessage = extractErrorMessage(response.body());
        String detail = apiMessage == null ? "" : " Gemini said: " + apiMessage;

        String explanation = switch (status) {
            case 400 -> "Gemini rejected the request (400). Usually a malformed payload, an oversized "
                    + "inline PDF, or an unsupported model for this request shape.";
            case 401 -> "Gemini rejected the API key (401). Check that GEMINI_API_KEY is a current key "
                    + "from https://aistudio.google.com/apikey, that it is sent in the '"
                    + API_KEY_HEADER + "' header (this client does), and that the Generative Language "
                    + "API is enabled on its Google Cloud project.";
            case 403 -> "Gemini denied access (403). The key is valid but restricted - check the key's "
                    + "API restrictions (it must allow the Generative Language API) and any "
                    + "IP/referrer restrictions in the Cloud Console.";
            case 404 -> "Gemini has no model named '" + model + "' (404). Model names change and old "
                    + "ones are retired - set GEMINI_MODEL to a current model (e.g. gemini-3.5-flash) "
                    + "from https://ai.google.dev/gemini-api/docs/models.";
            case 429 -> "Gemini rate limit or quota exhausted (429). Retry later or raise the quota on "
                    + "the key's project.";
            default -> status >= 500
                    ? "Gemini is unavailable (HTTP " + status + ")."
                    : "Gemini returned HTTP " + status + ".";
        };

        log.error("Gemini call failed: {}{}", explanation, detail);
        return new GeminiException(explanation + detail);
    }

    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode error = objectMapper.readTree(body).path("error");
            String message = error.path("message").asText(null);
            String status = error.path("status").asText(null);
            if (message == null) {
                return truncate(body);
            }
            return status == null ? message : message + " [" + status + "]";
        } catch (Exception ex) {
            return truncate(body);
        }
    }

    private static String truncate(String value) {
        return value.length() > 500 ? value.substring(0, 500) + "..." : value;
    }

    private JsonNode parseModelJson(String rawResponse) {
        JsonNode responseTree;
        try {
            responseTree = objectMapper.readTree(rawResponse);
        } catch (Exception ex) {
            throw new GeminiException("Gemini returned an unparseable response", ex);
        }

        JsonNode candidate = responseTree.path("candidates").path(0);
        String modelText = firstTextPart(candidate);

        if (modelText == null || modelText.isBlank()) {
            String blockReason = responseTree.path("promptFeedback").path("blockReason").asText(null);
            String finishReason = candidate.path("finishReason").asText(null);
            if (blockReason != null) {
                throw new GeminiException("Gemini declined to respond: " + blockReason);
            }
            if ("MAX_TOKENS".equals(finishReason)) {
                throw new GeminiException("Gemini hit its output limit before returning valid JSON.");
            }
            throw new GeminiException("Gemini returned an empty response"
                    + (finishReason != null ? " (finishReason=" + finishReason + ")" : ""));
        }

        String json = stripCodeFence(modelText);
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new GeminiException("Gemini's response wasn't valid JSON: " + truncate(json), ex);
        }
    }

    /**
     * A response can legitimately contain several parts (for instance a thought
     * summary followed by the answer), so take the first one that actually has
     * text rather than assuming parts[0].
     */
    private String firstTextPart(JsonNode candidate) {
        for (JsonNode part : candidate.path("content").path("parts")) {
            String text = part.path("text").asText(null);
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    /**
     * responseMimeType=application/json normally prevents this, but models
     * still occasionally wrap output in code fences. Cheap to tolerate,
     * expensive to debug.
     */
    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline < 0) {
            return trimmed;
        }
        String withoutOpening = trimmed.substring(firstNewline + 1);
        int closing = withoutOpening.lastIndexOf("```");
        return (closing >= 0 ? withoutOpening.substring(0, closing) : withoutOpening).trim();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GeminiException("Interrupted while waiting to retry the Gemini call", ex);
        }
    }

    private record Response(HttpStatusCode status, String body) {
    }
}
