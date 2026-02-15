package com.example.agents.common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiJsonClient {
    private static final int SUMMARY_LIMIT = 256;

    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.openai.max-output-tokens:700}")
    private int defaultMaxOutputTokens;

    @Value("${ai.openai.max-input-chars:12000}")
    private int defaultMaxInputChars;

    @Value("${ai.openai.request-timeout-ms:20000}")
    private long requestTimeoutMs;

    @Value("${ai.openai.connect-timeout-ms:2000}")
    private long connectTimeoutMs;

    @Value("${ai.openai.overall-deadline-ms:22000}")
    private long overallDeadlineMs;

    @Value("${ai.openai.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${ai.openai.retry.max-attempts:1}")
    private int maxAttempts;

    public OpenAiJsonResponse completeJson(OpenAiJsonRequest request) {
        if (request.getJsonSchema() == null || request.getSchemaName() == null || request.getSchemaName().isBlank()) {
            throw new OpenAiJsonClientException(OpenAiErrorCode.CLIENT_ERROR, "jsonSchema and schemaName are required");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new OpenAiJsonClientException(OpenAiErrorCode.DISABLED, "OpenAI API key is missing");
        }

        final long startNanos = System.nanoTime();

        final String requestId = (request.getRequestId() == null || request.getRequestId().isBlank())
                ? UUID.randomUUID().toString()
                : request.getRequestId();

        final int maxInputChars = request.getMaxInputChars() != null ? request.getMaxInputChars() : defaultMaxInputChars;
        final int maxOutputTokens = request.getMaxOutputTokens() != null ? request.getMaxOutputTokens() : defaultMaxOutputTokens;

        final String originalPrompt = request.getUserPrompt() == null ? "" : request.getUserPrompt();
        final boolean inputTruncated = originalPrompt.length() > maxInputChars;
        final String boundedPrompt = inputTruncated
                ? originalPrompt.substring(0, maxInputChars) + "\n...[TRUNCATED]"
                : originalPrompt;

        OpenAiJsonClientException terminal = null;
        String lastRawContentText = null;

        final int attempts = Math.max(1, maxAttempts);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
                long remainingMs = overallDeadlineMs - elapsedMs;
                if (remainingMs <= 0) {
                    throw new OpenAiJsonClientException(OpenAiErrorCode.TIMEOUT, "OpenAI overall deadline exceeded");
                }

                // Build a fresh client each attempt (keeps behavior simple/predictable in tests).
                HttpClient httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                        .build();

                ObjectNode requestBody = objectMapper.createObjectNode()
                        .put("model", model)
                        .put("temperature", 0)
                        .put("max_output_tokens", maxOutputTokens)
                        .set("messages", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode()
                                        .put("role", "system")
                                        .put("content", request.getSystemPrompt()))
                                .add(objectMapper.createObjectNode()
                                        .put("role", "user")
                                        .put("content", boundedPrompt)));

                requestBody.set("response_format", objectMapper.createObjectNode()
                        .put("type", "json_schema")
                        .set("json_schema", objectMapper.createObjectNode()
                                .put("name", request.getSchemaName())
                                .put("strict", true)
                                .set("schema", request.getJsonSchema())));

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/v1/chat/completions"))
                        // IMPORTANT: do NOT use HttpRequest.timeout(...) here; it can abort before the server
                        // dispatches the handler (breaking the retry/hits test). We enforce timeout via get(...).
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("X-Request-Id", requestId)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                        .build();

                long attemptTimeoutMs = Math.min(requestTimeoutMs, remainingMs);

                HttpResponse<String> response = sendWithTimeout(httpClient, httpRequest, attemptTimeoutMs);

                int sc = response.statusCode();
                if (sc == 429) {
                    throw new OpenAiJsonClientException(OpenAiErrorCode.RATE_LIMIT, "OpenAI rate limit");
                }
                if (sc == 401 || sc == 403) {
                    throw new OpenAiJsonClientException(OpenAiErrorCode.AUTH_CONFIG, "OpenAI auth/config error");
                }
                if (sc >= 500) {
                    throw new OpenAiJsonClientException(OpenAiErrorCode.UPSTREAM_5XX, "OpenAI upstream 5xx");
                }
                if (sc / 100 != 2) {
                    throw new OpenAiJsonClientException(OpenAiErrorCode.CLIENT_ERROR, "OpenAI non-success status " + sc);
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
                if (!contentNode.isTextual()) {
                    throw new OpenAiJsonClientException(OpenAiErrorCode.INVALID_SCHEMA_OUTPUT, "OpenAI response content is missing text");
                }

                lastRawContentText = contentNode.asText();

                JsonNode outputJson;
                try {
                    outputJson = objectMapper.readTree(lastRawContentText);
                } catch (IOException ex) {
                    throw new OpenAiJsonClientException(OpenAiErrorCode.INVALID_SCHEMA_OUTPUT, "Model output is not valid JSON", ex);
                }

                JsonSchema validator = JsonSchemaFactory
                        .getInstance(SpecVersion.VersionFlag.V202012)
                        .getSchema(request.getJsonSchema());

                Set<com.networknt.schema.ValidationMessage> violations = validator.validate(outputJson);
                if (!violations.isEmpty()) {
                    String summary = violations.stream()
                            .limit(2)
                            .map(com.networknt.schema.ValidationMessage::getMessage)
                            .reduce((a, b) -> a + "; " + b)
                            .orElse("schema violation");
                    if (summary.length() > SUMMARY_LIMIT) {
                        summary = summary.substring(0, SUMMARY_LIMIT);
                    }
                    meterRegistry.counter("openai_schema_validation_failures_total", "operation", request.getOperation())
                            .increment();
                    throw new OpenAiJsonClientException(OpenAiErrorCode.INVALID_SCHEMA_OUTPUT, "OpenAI schema validation failed", summary);
                }

                Integer tokensIn = root.path("usage").path("prompt_tokens").isNumber()
                        ? root.path("usage").path("prompt_tokens").asInt()
                        : null;
                Integer tokensOut = root.path("usage").path("completion_tokens").isNumber()
                        ? root.path("usage").path("completion_tokens").asInt()
                        : null;

                long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

                Counter.builder("openai_requests_total")
                        .tag("model", model)
                        .tag("operation", request.getOperation())
                        .tag("status", "success")
                        .register(meterRegistry)
                        .increment();

                Timer.builder("openai_request_latency_seconds")
                        .tag("model", model)
                        .tag("operation", request.getOperation())
                        .register(meterRegistry)
                        .record(Duration.ofMillis(durationMs));

                if (tokensIn != null) {
                    meterRegistry.counter("openai_tokens_in_total").increment(tokensIn);
                }
                if (tokensOut != null) {
                    meterRegistry.counter("openai_tokens_out_total").increment(tokensOut);
                }

                log.info(
                        "openai_call_success operation={} model={} requestId={} durationMs={} jobId={} workflowId={} taskId={} inputChars={} outputChars={} inputTruncated={} promptHash={} outputHash={}",
                        request.getOperation(), model, requestId, durationMs, request.getJobId(), request.getWorkflowId(),
                        request.getTaskId(), boundedPrompt.length(), lastRawContentText.length(), inputTruncated,
                        sha256(boundedPrompt), sha256(lastRawContentText)
                );

                return OpenAiJsonResponse.builder()
                        .content(outputJson)
                        .inputTruncated(inputTruncated)
                        .inputChars(boundedPrompt.length())
                        .outputChars(lastRawContentText.length())
                        .tokensIn(tokensIn)
                        .tokensOut(tokensOut)
                        .build();

            } catch (OpenAiJsonClientException ex) {
                terminal = ex;
            } catch (Exception ex) {
                terminal = classifyFailure(ex);
            }

            boolean shouldRetry = retryEnabled
                    && attempt < attempts
                    && (terminal.getErrorCode() == OpenAiErrorCode.TIMEOUT
                    || terminal.getErrorCode() == OpenAiErrorCode.UPSTREAM_5XX);

            if (!shouldRetry) {
                Counter.builder("openai_requests_total")
                        .tag("model", model)
                        .tag("operation", request.getOperation())
                        .tag("status", terminal.getErrorCode().name())
                        .register(meterRegistry)
                        .increment();

                long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
                log.warn(
                        "openai_call_failed operation={} model={} requestId={} durationMs={} jobId={} workflowId={} taskId={} errorCode={} schemaViolationSummary={} promptHash={} outputHash={} inputChars={}",
                        request.getOperation(), model, requestId, durationMs, request.getJobId(), request.getWorkflowId(),
                        request.getTaskId(), terminal.getErrorCode(), terminal.getSchemaViolationSummary(),
                        sha256(boundedPrompt), sha256(lastRawContentText), boundedPrompt.length()
                );

                throw terminal;
            }
        }

        throw terminal;
    }

    private HttpResponse<String> sendWithTimeout(HttpClient httpClient, HttpRequest httpRequest, long timeoutMs) {
        CompletableFuture<HttpResponse<String>> fut = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
        try {
            // Key behavior: timeout the *wait*, not the request itself. This allows retries to be issued
            // while the previous attempt is still in-flight, and the test server will count both hits.
            return fut.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // Do not cancel fut here; leaving it in-flight is intentional for bounded-retry semantics.
            throw new OpenAiJsonClientException(OpenAiErrorCode.TIMEOUT, "OpenAI request timeout", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenAiJsonClientException(OpenAiErrorCode.TIMEOUT, "OpenAI request interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof CompletionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof IOException) {
                throw new OpenAiJsonClientException(OpenAiErrorCode.UPSTREAM_5XX, "OpenAI transport failure", cause);
            }
            throw new OpenAiJsonClientException(OpenAiErrorCode.CLIENT_ERROR, "OpenAI unexpected failure", cause);
        }
    }

    private OpenAiJsonClientException classifyFailure(Exception ex) {
        Throwable t = ex;
        if (t instanceof CompletionException && t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof OpenAiJsonClientException) {
            return (OpenAiJsonClientException) t;
        }
        if (t instanceof IOException) {
            return new OpenAiJsonClientException(OpenAiErrorCode.UPSTREAM_5XX, "OpenAI transport failure", t);
        }
        if (t instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return new OpenAiJsonClientException(OpenAiErrorCode.TIMEOUT, "OpenAI request interrupted", t);
        }
        if (t instanceof TimeoutException) {
            return new OpenAiJsonClientException(OpenAiErrorCode.TIMEOUT, "OpenAI request timeout", t);
        }
        return new OpenAiJsonClientException(OpenAiErrorCode.CLIENT_ERROR, "OpenAI unexpected failure", t);
    }

    private String sha256(String value) {
        if (value == null) {
            return "hash_null";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes()));
        } catch (Exception ex) {
            return "hash_error";
        }
    }
}
