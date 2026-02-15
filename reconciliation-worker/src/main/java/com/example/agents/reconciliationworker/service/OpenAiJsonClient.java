package com.example.agents.reconciliationworker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiJsonClient {
    private final ObjectMapper objectMapper;

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;

    public Optional<String> completeJson(String systemPrompt, String userPrompt, JsonNode jsonSchema) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode requestBody = objectMapper.createObjectNode()
                    .put("model", model)
                    .put("temperature", 0)
                    .set("messages", objectMapper.createArrayNode()
                            .add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt))
                            .add(objectMapper.createObjectNode().put("role", "user").put("content", userPrompt)));
            ((com.fasterxml.jackson.databind.node.ObjectNode) requestBody)
                    .set("response_format", objectMapper.createObjectNode()
                            .put("type", "json_schema")
                            .set("json_schema", objectMapper.createObjectNode()
                                    .put("name", "reconciliation_reasoning_response")
                                    .put("strict", true)
                                    .set("schema", jsonSchema)));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(30))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("OpenAI call failed with status {}", response.statusCode());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                return Optional.empty();
            }
            return Optional.of(contentNode.asText());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("OpenAI request interrupted", ex);
            return Optional.empty();
        } catch (IOException ex) {
            log.warn("OpenAI request failed", ex);
            return Optional.empty();
        }
    }
}
