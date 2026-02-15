package com.example.agents.classificationworker.service;

import com.example.agents.classificationworker.dto.PipelineStepRequestDto;
import com.example.agents.classificationworker.entity.PipelineStepEntity;
import com.example.agents.classificationworker.mapper.IPipelineStepMapper;
import com.example.agents.classificationworker.repository.IPipelineStepRepository;
import com.example.agents.common.ai.OpenAiJsonClient;
import com.example.agents.common.ai.OpenAiJsonClientException;
import com.example.agents.common.ai.OpenAiJsonRequest;
import com.example.agents.common.ai.OpenAiJsonResponse;
import com.example.agents.common.ai.PipelineTaskException;
import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.common.enums.PipelineStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineStepService implements IPipelineStepService {
    private final IPipelineStepRepository pipelineStepRepository;
    private final IPipelineStepMapper pipelineStepMapper;
    private final OpenAiJsonClient openAiJsonClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Value("${ai.operations.classification.max-text-chars:12000}")
    private int maxTextChars;

    @Override
    @Transactional
    public PipelineMessageDto process(PipelineStepRequestDto requestDto) {
        String idempotencyKey = requestDto.getJobId() + ":" + requestDto.getTaskType();
        PipelineStepEntity entity = pipelineStepRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> pipelineStepMapper.toEntity(requestDto));

        String payload = entity.getId() == null && (entity.getPayloadJson() == null || entity.getPayloadJson().isBlank())
                ? classifyPayload(requestDto)
                : requestDto.getPayloadJson();

        entity.setPayloadJson(payload);
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

    private String classifyPayload(PipelineStepRequestDto requestDto) {
        try {
            JsonNode payload = objectMapper.readTree(requestDto.getPayloadJson());
            String text = payload.path("text").asText("").trim();
            if (text.isBlank()) {
                throw new PipelineTaskException("INVALID_INPUT", "classification payload text is blank");
            }
            boolean inputTruncated = text.length() > maxTextChars;
            String boundedText = inputTruncated ? text.substring(0, maxTextChars) : text;
            List<String> labels = extractLabels(payload.path("candidateLabels"));
            JsonNode schema = classificationSchema(labels);
            String userPrompt = "Classify the text into one label from: " + labels + "\nText:\n" + boundedText +
                    "\nReturn confidence between 0 and 1.";

            OpenAiJsonResponse response = openAiJsonClient.completeJson(OpenAiJsonRequest.builder()
                    .operation("classification")
                    .schemaName("classification_response")
                    .systemPrompt(classificationSystemPrompt(labels))
                    .userPrompt(userPrompt)
                    .jsonSchema(schema)
                    .requestId(UUID.randomUUID().toString())
                    .jobId(requestDto.getJobId())
                    .taskId(requestDto.getTaskType())
                    .maxInputChars(maxTextChars)
                    .build());
            ClassificationResult result = objectMapper.treeToValue(response.getContent(), ClassificationResult.class);
            validateOutput(result);
            ObjectNode output = objectMapper.valueToTree(result);
            output.put("input_truncated", inputTruncated || response.isInputTruncated());
            return objectMapper.writeValueAsString(output);
        } catch (OpenAiJsonClientException ex) {
            throw new PipelineTaskException(ex.getErrorCode().name(), "classification LLM call failed", ex);
        } catch (PipelineTaskException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PipelineTaskException("INVALID_INPUT", "classification payload parse failed", ex);
        }
    }

    private void validateOutput(ClassificationResult result) {
        Set<ConstraintViolation<ClassificationResult>> violations = validator.validate(result);
        if (!violations.isEmpty()) {
            throw new PipelineTaskException("INVALID_OUTPUT", violations.iterator().next().getMessage());
        }
    }

    private List<String> extractLabels(JsonNode labelsNode) {
        List<String> labels = new ArrayList<>();
        if (labelsNode != null && labelsNode.isArray()) {
            for (JsonNode node : labelsNode) {
                labels.add(node.asText());
            }
        }
        return labels.isEmpty()
                ? List.of("invoice", "receipt", "bank_statement", "tax_document", "other")
                : labels;
    }

    private String classificationSystemPrompt(List<String> labels) {
        return "You are a senior financial document classification specialist. " +
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
        reasonNode.put("minLength", 3);
        properties.set("reason", reasonNode);

        schema.set("properties", properties);
        schema.set("required", objectMapper.valueToTree(List.of("label", "confidence", "reason")));
        schema.put("additionalProperties", false);

        return schema;
    }

    @Data
    @Builder
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder = ClassificationResult.ClassificationResultBuilder.class)
    private static class ClassificationResult {
        @NotBlank String label;
        @DecimalMin("0.0") @DecimalMax("1.0") double confidence;
        @NotBlank String reason;

        @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
        public static class ClassificationResultBuilder {}
    }
}
