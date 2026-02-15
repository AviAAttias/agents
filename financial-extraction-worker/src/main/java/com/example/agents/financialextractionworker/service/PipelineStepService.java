package com.example.agents.financialextractionworker.service;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.common.enums.PipelineStatus;
import com.example.agents.financialextractionworker.dto.PipelineStepRequestDto;
import com.example.agents.financialextractionworker.entity.PipelineStepEntity;
import com.example.agents.financialextractionworker.mapper.IPipelineStepMapper;
import com.example.agents.financialextractionworker.repository.IPipelineStepRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PipelineStepService implements IPipelineStepService {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+[.,]\\d{2})");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("\\b(USD|EUR|GBP|SAR|AED)\\b", Pattern.CASE_INSENSITIVE);

    private final IPipelineStepRepository pipelineStepRepository;
    private final IPipelineStepMapper pipelineStepMapper;
    private final OpenAiJsonClient openAiJsonClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PipelineMessageDto process(PipelineStepRequestDto requestDto) {
        PipelineStepEntity entity = pipelineStepRepository.findByIdempotencyKey(requestDto.getJobId() + ":" + requestDto.getTaskType())
                .orElseGet(() -> pipelineStepMapper.toEntity(requestDto));
        entity.setPayloadJson(extractFinancialData(requestDto.getPayloadJson()));
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

    private String extractFinancialData(String payloadJson) {
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
        properties.set("explanation", objectMapper.createObjectNode().put("type", "string"));

        schema.set("properties", properties);
        schema.set("required", objectMapper.valueToTree(List.of(
                "documentType", "currency", "totalAmount", "confidence", "explanation"
        )));
        schema.put("additionalProperties", false);

        String systemPrompt = "You are a financial data extraction specialist. Extract structured values from OCR text. " +
                "Be conservative with confidence and provide a short explanation of signal quality.";
        String userPrompt = "Extract fields from this payload and return strictly valid JSON.\nPayload:\n" + payloadJson;

        return openAiJsonClient.completeJson(systemPrompt, userPrompt, schema)
                .orElseGet(() -> deterministicFallback(payloadJson));
    }

    private String deterministicFallback(String payloadJson) {
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(payloadJson);
        Matcher currencyMatcher = CURRENCY_PATTERN.matcher(payloadJson);
        BigDecimal totalAmount = amountMatcher.find()
                ? new BigDecimal(amountMatcher.group(1).replace(',', '.'))
                : BigDecimal.ZERO;
        String currency = currencyMatcher.find() ? currencyMatcher.group(1).toUpperCase() : "UNKNOWN";

        try {
            return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("documentType", "unknown")
                    .put("invoiceNumber", "")
                    .put("currency", currency)
                    .put("totalAmount", totalAmount)
                    .put("taxAmount", 0)
                    .put("dueDate", "")
                    .put("confidence", 0.35)
                    .put("explanation", "Deterministic fallback parser used because LLM output was unavailable."));
        } catch (JsonProcessingException ex) {
            return "{\"documentType\":\"unknown\",\"currency\":\"UNKNOWN\",\"totalAmount\":0," +
                    "\"confidence\":0,\"explanation\":\"fallback serialization error\"}";
        }
    }
}
