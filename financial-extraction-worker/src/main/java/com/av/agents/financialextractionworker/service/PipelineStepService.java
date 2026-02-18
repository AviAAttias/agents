package com.av.agents.financialextractionworker.service;

import com.av.agents.common.ai.OpenAiJsonClient;
import com.av.agents.common.ai.OpenAiJsonClientException;
import com.av.agents.common.ai.OpenAiJsonRequest;
import com.av.agents.common.ai.OpenAiJsonResponse;
import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.common.enums.PipelineStatus;
import com.av.agents.financialextractionworker.dto.PipelineStepRequestDto;
import com.av.agents.financialextractionworker.entity.PipelineStepEntity;
import com.av.agents.financialextractionworker.mapper.IPipelineStepMapper;
import com.av.agents.financialextractionworker.repository.IPipelineStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PipelineStepService implements IPipelineStepService {
    private final IPipelineStepRepository pipelineStepRepository;
    private final IPipelineStepMapper pipelineStepMapper;
    private final OpenAiJsonClient openAiJsonClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Value("${ai.operations.financial-extraction.max-text-chars:12000}")
    private int maxTextChars;

    @Override
    @Transactional
    public PipelineMessageDto process(PipelineStepRequestDto requestDto) {
        PipelineStepEntity entity = pipelineStepRepository.findByIdempotencyKey(requestDto.getJobId() + ":" + requestDto.getTaskType())
                .orElseGet(() -> pipelineStepMapper.toEntity(requestDto));
        entity.setPayloadJson(extractFinancialData(requestDto));
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

    private String extractFinancialData(PipelineStepRequestDto requestDto) {
        if (requestDto.getPayloadJson() == null || requestDto.getPayloadJson().isBlank()) {
            throw new PipelineTaskException("INVALID_INPUT", "financial extraction payload is blank");
        }
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("documentType", objectMapper.createObjectNode().put("type", "string"));
        properties.set("invoiceNumber", objectMapper.createObjectNode().put("type", "string"));
        properties.set("currency", objectMapper.createObjectNode().put("type", "string"));
        properties.set("totalAmount", objectMapper.createObjectNode().put("type", "number"));
        properties.set("taxAmount", objectMapper.createObjectNode().put("type", "number"));
        properties.set("dueDate", objectMapper.createObjectNode().put("type", "string"));
        properties.set("confidence", objectMapper.createObjectNode().put("type", "number").put("minimum", 0).put("maximum", 1));
        properties.set("explanation", objectMapper.createObjectNode().put("type", "string").put("minLength", 3));

        schema.set("properties", properties);
        schema.set("required", objectMapper.valueToTree(List.of(
                "documentType", "currency", "totalAmount", "confidence", "explanation"
        )));
        schema.put("additionalProperties", false);

        String boundedPayload = requestDto.getPayloadJson().length() > maxTextChars
                ? requestDto.getPayloadJson().substring(0, maxTextChars)
                : requestDto.getPayloadJson();
        String systemPrompt = "You are a financial data extraction specialist. Extract structured values from OCR text.";
        String userPrompt = "Extract fields from this payload and return strictly valid JSON.\nPayload:\n" + boundedPayload;

        try {
            OpenAiJsonResponse response = openAiJsonClient.completeJson(OpenAiJsonRequest.builder()
                    .operation("financial_extraction")
                    .schemaName("financial_extraction_response")
                    .systemPrompt(systemPrompt)
                    .userPrompt(userPrompt)
                    .jsonSchema(schema)
                    .requestId(UUID.randomUUID().toString())
                    .jobId(requestDto.getJobId())
                    .taskId(requestDto.getTaskType())
                    .maxInputChars(maxTextChars)
                    .build());
            FinancialExtractionResult result = objectMapper.treeToValue(response.getContent(), FinancialExtractionResult.class);
            validateOutput(result);
            ObjectNode output = objectMapper.valueToTree(result);
            output.put("input_truncated", response.isInputTruncated() || requestDto.getPayloadJson().length() > maxTextChars);
            output.put("chunk_count", 1);
            return objectMapper.writeValueAsString(output);
        } catch (OpenAiJsonClientException ex) {
            throw new PipelineTaskException(ex.getErrorCode().name(), "financial extraction LLM call failed", ex);
        } catch (PipelineTaskException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PipelineTaskException("INVALID_OUTPUT", "financial extraction result could not be validated", ex);
        }
    }

    private void validateOutput(FinancialExtractionResult result) {
        Set<ConstraintViolation<FinancialExtractionResult>> violations = validator.validate(result);
        if (!violations.isEmpty()) {
            throw new PipelineTaskException("INVALID_OUTPUT", violations.iterator().next().getMessage());
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class FinancialExtractionResult {
        @NotBlank
        private String documentType;
        private String invoiceNumber;

        @NotBlank
        private String currency;

        @NotNull
        private BigDecimal totalAmount;
        private BigDecimal taxAmount;

        private String dueDate;

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double confidence;

        @NotBlank
        private String explanation;
    }
}
