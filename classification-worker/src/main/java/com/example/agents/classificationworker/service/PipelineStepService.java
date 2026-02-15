package com.example.agents.classificationworker.service;

import com.example.agents.classificationworker.dto.PipelineStepRequestDto;
import com.example.agents.classificationworker.entity.PipelineStepEntity;
import com.example.agents.classificationworker.mapper.IPipelineStepMapper;
import com.example.agents.classificationworker.repository.IPipelineStepRepository;
import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.common.enums.PipelineStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineStepService implements IPipelineStepService {
    private final IPipelineStepRepository pipelineStepRepository;
    private final IPipelineStepMapper pipelineStepMapper;
    private final OpenAiJsonClient openAiJsonClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PipelineMessageDto process(PipelineStepRequestDto requestDto) {
        PipelineStepEntity entity = pipelineStepRepository.findByIdempotencyKey(requestDto.getJobId() + ":" + requestDto.getTaskType())
                .orElseGet(() -> pipelineStepMapper.toEntity(requestDto));

        entity.setPayloadJson(classifyPayload(requestDto.getPayloadJson()));
        entity.setStatus(PipelineStatus.PROCESSED.name());
        entity.setUpdatedAt(OffsetDateTime.now());
        pipelineStepRepository.save(entity);
        return PipelineMessageDto.builder()
                .jobId(entity.getJobId())
                .taskType(entity.getTaskType())
                .status(PipelineStatus.PROCESSED)
                .artifactRef(entity.getArtifactRef())
                .payloadJson(entity.getPayloadJson())
                .processedAt(entity.getUpdatedAt())
                .build();
    }

    private String classifyPayload(String payloadJson) {
        try {
            JsonNode payload = objectMapper.readTree(payloadJson);
            String text = payload.path("text").asText(payloadJson);
            List<String> labels = extractLabels(payload.path("candidateLabels"));
            JsonNode schema = classificationSchema(labels);
            String userPrompt = "Classify the text into one label from: " + labels + "\nText:\n" + text +
                    "\nReturn confidence between 0 and 1.";

            return openAiJsonClient.completeJson(classificationSystemPrompt(labels), userPrompt, schema)
                    .orElseGet(() -> fallbackClassification(text, labels));
        } catch (Exception ex) {
            return fallbackClassification(payloadJson, List.of("invoice", "receipt", "bank_statement", "other"));
        }
    }

    private List<String> extractLabels(JsonNode labelsNode) {
        List<String> labels = new ArrayList<>();
        if (labelsNode.isArray()) {
            labelsNode.forEach(node -> labels.add(node.asText()));
        }
        if (labels.isEmpty()) {
            labels = List.of("invoice", "receipt", "bank_statement", "tax_document", "other");
        }
        return labels;
    }

    private String classificationSystemPrompt(List<String> labels) {
        return "You are a senior financial document classification specialist. " +
                "Use zero-shot reasoning over the allowed labels and apply few-shot style guidance: " +
                "1) 'Total due by 2024-01-31' -> invoice. " +
                "2) 'Point of sale card payment with VAT' -> receipt. " +
                "3) 'Opening balance and account transactions' -> bank_statement. " +
                "Output only JSON matching schema and never invent labels outside: " + labels;
    }

    private ObjectNode classificationSchema(List<String> labels) {

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode labelNode = objectMapper.createObjectNode();
        labelNode.put("type", "string");
        labelNode.set("enum", objectMapper.valueToTree(labels));
        properties.set("label", labelNode);

        ObjectNode confidenceNode = objectMapper.createObjectNode();
        confidenceNode.put("type", "number");
        confidenceNode.put("minimum", 0);
        confidenceNode.put("maximum", 1);
        properties.set("confidence", confidenceNode);

        ObjectNode reasonNode = objectMapper.createObjectNode();
        reasonNode.put("type", "string");
        properties.set("reason", reasonNode);

        schema.set("properties", properties);
        schema.set("required", objectMapper.valueToTree(List.of("label", "confidence", "reason")));
        schema.put("additionalProperties", false);

        return schema;
    }

    private String fallbackClassification(String text, List<String> labels) {
        String lowered = text.toLowerCase();
        String label = labels.contains("invoice") && (lowered.contains("invoice") || lowered.contains("total due")) ? "invoice"
                : labels.contains("receipt") && lowered.contains("receipt") ? "receipt"
                : labels.contains("bank_statement") && lowered.contains("balance") ? "bank_statement"
                : labels.get(labels.size() - 1);
        try {
            return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("label", label)
                    .put("confidence", 0.45)
                    .put("reason", "Fallback keyword classification used because LLM response was unavailable."));
        } catch (JsonProcessingException ex) {
            return "{\"label\":\"other\",\"confidence\":0.0,\"reason\":\"fallback serialization error\"}";
        }
    }
}
