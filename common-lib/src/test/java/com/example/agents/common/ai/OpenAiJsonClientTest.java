package com.example.agents.common.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiJsonClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validSchemaCompliantJsonReturnsSuccess() throws Exception {
        HttpServer server = startServer(200, "{\"choices\":[{\"message\":{\"content\":\"{\\\"label\\\":\\\"invoice\\\",\\\"confidence\\\":0.9}\"}}],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":8}}", 0, null);
        try {
            OpenAiJsonClient client = createClient(server);
            OpenAiJsonResponse response = client.completeJson(baseRequest());
            assertThat(response.getContent().path("label").asText()).isEqualTo("invoice");
            assertThat(response.getTokensIn()).isEqualTo(12);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void invalidJsonReturnsInvalidSchemaOutput() throws Exception {
        HttpServer server = startServer(200, "{\"choices\":[{\"message\":{\"content\":\"not-json\"}}]}", 0, null);
        try {
            OpenAiJsonClient client = createClient(server);
            OpenAiJsonClientException ex = assertThrows(OpenAiJsonClientException.class, () -> client.completeJson(baseRequest()));
            assertThat(ex.getErrorCode()).isEqualTo(OpenAiErrorCode.INVALID_SCHEMA_OUTPUT);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void schemaViolationReturnsSummary() throws Exception {
        HttpServer server = startServer(200, "{\"choices\":[{\"message\":{\"content\":\"{\\\"label\\\":123}\"}}]}", 0, null);
        try {
            OpenAiJsonClient client = createClient(server);
            OpenAiJsonClientException ex = assertThrows(OpenAiJsonClientException.class, () -> client.completeJson(baseRequest()));
            assertThat(ex.getErrorCode()).isEqualTo(OpenAiErrorCode.INVALID_SCHEMA_OUTPUT);
            assertThat(ex.getSchemaViolationSummary()).isNotBlank();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void statusCodesClassified() throws Exception {
        assertClassified(429, OpenAiErrorCode.RATE_LIMIT);
        assertClassified(500, OpenAiErrorCode.UPSTREAM_5XX);
    }

    @Test
    void timeoutClassifiedAndRetryBounded() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = startServer(200, "{\"choices\":[{\"message\":{\"content\":\"{\\\"label\\\":\\\"invoice\\\",\\\"confidence\\\":0.9}\"}}]}", 200, hits);
        try {
            OpenAiJsonClient client = createClient(server);
            ReflectionTestUtils.setField(client, "requestTimeoutMs", 50L);
            ReflectionTestUtils.setField(client, "maxAttempts", 2);
            ReflectionTestUtils.setField(client, "retryEnabled", true);
            OpenAiJsonClientException ex = assertThrows(OpenAiJsonClientException.class, () -> client.completeJson(baseRequest()));
            assertThat(ex.getErrorCode()).isEqualTo(OpenAiErrorCode.TIMEOUT);
            assertThat(hits.get()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }

    private void assertClassified(int status, OpenAiErrorCode expected) throws Exception {
        HttpServer server = startServer(status, "{}", 0, null);
        try {
            OpenAiJsonClient client = createClient(server);
            OpenAiJsonClientException ex = assertThrows(OpenAiJsonClientException.class, () -> client.completeJson(baseRequest()));
            assertThat(ex.getErrorCode()).isEqualTo(expected);
        } finally {
            server.stop(0);
        }
    }

    private OpenAiJsonRequest baseRequest() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();
        props.set("label", objectMapper.createObjectNode().put("type", "string"));
        props.set("confidence", objectMapper.createObjectNode().put("type", "number"));
        schema.set("properties", props);
        schema.putArray("required").add("label").add("confidence");
        schema.put("additionalProperties", false);
        return OpenAiJsonRequest.builder()
                .operation("classification")
                .schemaName("classification_response")
                .systemPrompt("system")
                .userPrompt("user")
                .jsonSchema(schema)
                .jobId("job-1")
                .taskId("task-1")
                .build();
    }

    private OpenAiJsonClient createClient(HttpServer server) {
        OpenAiJsonClient client = new OpenAiJsonClient(objectMapper, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:" + server.getAddress().getPort());
        ReflectionTestUtils.setField(client, "model", "gpt-4o-mini");
        ReflectionTestUtils.setField(client, "requestTimeoutMs", 1000L);
        ReflectionTestUtils.setField(client, "connectTimeoutMs", 500L);
        ReflectionTestUtils.setField(client, "overallDeadlineMs", 2000L);
        ReflectionTestUtils.setField(client, "defaultMaxInputChars", 5000);
        ReflectionTestUtils.setField(client, "defaultMaxOutputTokens", 200);
        ReflectionTestUtils.setField(client, "retryEnabled", true);
        ReflectionTestUtils.setField(client, "maxAttempts", 1);
        return client;
    }

    private HttpServer startServer(int status, String responseBody, long delayMs, AtomicInteger hits) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> handle(exchange, status, responseBody, delayMs, hits));
        server.start();
        return server;
    }

    private void handle(HttpExchange exchange, int status, String responseBody, long delayMs, AtomicInteger hits) throws IOException {
        if (hits != null) {
            hits.incrementAndGet();
        }
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, out.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
        }
    }
}
