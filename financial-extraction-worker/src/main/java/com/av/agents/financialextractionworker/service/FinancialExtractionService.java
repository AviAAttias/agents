package com.av.agents.financialextractionworker.service;

import com.av.agents.common.ai.OpenAiErrorCode;
import com.av.agents.common.ai.OpenAiJsonClient;
import com.av.agents.common.ai.OpenAiJsonClientException;
import com.av.agents.common.ai.OpenAiJsonRequest;
import com.av.agents.common.ai.OpenAiJsonResponse;
import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.financialextractionworker.dto.FinancialExtractionRequestDto;
import com.av.agents.financialextractionworker.dto.FinancialExtractionResultDto;
import com.av.agents.sharedpersistence.entity.FinancialArtifactEntity;
import com.av.agents.sharedpersistence.repository.IFinancialArtifactRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialExtractionService implements IFinancialExtractionService {
    private static final String TASK_TYPE = "extract_financials";
    private static final String SCHEMA_VERSION = "v1";

    private final OpenAiJsonClient openAiJsonClient;
    private final IFinancialArtifactRepository financialArtifactRepository;
    private final ObjectMapper objectMapper;

    @Value("classpath:schema/financial-extraction-v1.json")
    private Resource financialSchemaResource;

    @Value("${ai.operations.financial-extraction.max-text-chars:12000}")
    private int maxTextChars;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String configuredModel;

    @Override
    @Transactional
    public FinancialExtractionResultDto extract(FinancialExtractionRequestDto request) {
        validateRequest(request);

        FinancialArtifactEntity existing = financialArtifactRepository.findByJobIdAndTaskType(request.getJobId(), TASK_TYPE)
                .orElse(null);
        if (existing != null) {
            return toResult(existing);
        }

        String normalizedDocType = request.getDocType().trim().toUpperCase(Locale.ROOT);
        if (!isSupportedFinancialDocType(normalizedDocType)) {
            throw new PipelineTaskException("INVALID_INPUT", "unsupported financial docType: " + normalizedDocType);
        }

        String rawText = request.getText();
        boolean wasTruncated = rawText.length() > maxTextChars;
        String boundedText = wasTruncated ? rawText.substring(0, maxTextChars) : rawText;

        long start = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        OpenAiJsonResponse aiResponse = callOpenAiWithSingleRetry(buildAiRequest(request, normalizedDocType, boundedText, requestId));
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        CanonicalFinancial canonical = mapCanonical(normalizedDocType, aiResponse.getContent());

        FinancialArtifactEntity entity = new FinancialArtifactEntity();
        entity.setJobId(request.getJobId());
        entity.setTaskType(TASK_TYPE);
        entity.setCanonicalJson(writeCanonicalJson(canonical));
        entity.setDocumentType(canonical.documentType());
        entity.setCurrency(canonical.currency());
        entity.setTotalAmount(canonical.totalAmount());
        entity.setPeriodStart(canonical.periodStart());
        entity.setPeriodEnd(canonical.periodEnd());
        entity.setInputTextSha256(sha256(boundedText));
        entity.setInputCharCount(rawText.length());
        entity.setWasTruncated(wasTruncated);
        entity.setModel(configuredModel);
        entity.setSchemaVersion(SCHEMA_VERSION);
        entity.setCreatedAt(OffsetDateTime.now());

        FinancialArtifactEntity saved = financialArtifactRepository.save(entity);

        log.info("event=financial_extraction_completed model={} durationMs={} inputChars={} outputChars={} requestId={}",
                configuredModel, durationMs, boundedText.length(), aiResponse.getOutputChars(), requestId);

        return toResult(saved);
    }

    private OpenAiJsonRequest buildAiRequest(FinancialExtractionRequestDto request,
                                             String normalizedDocType,
                                             String boundedText,
                                             String requestId) {
        return OpenAiJsonRequest.builder()
                .operation("financial_extraction")
                .schemaName("financial_extraction_v1")
                .systemPrompt("Extract canonical financial details from the document text.")
                .userPrompt("documentType=" + normalizedDocType + "\ntext=" + boundedText)
                .jsonSchema(loadSchema())
                .requestId(requestId)
                .jobId(request.getJobId())
                .workflowId(request.getWorkflowId())
                .taskId(request.getTaskId())
                .maxInputChars(maxTextChars)
                .build();
    }

    private OpenAiJsonResponse callOpenAiWithSingleRetry(OpenAiJsonRequest aiRequest) {
        try {
            return openAiJsonClient.completeJson(aiRequest);
        } catch (OpenAiJsonClientException ex) {
            if (ex.getErrorCode() == OpenAiErrorCode.TIMEOUT || ex.getErrorCode() == OpenAiErrorCode.UPSTREAM_5XX) {
                try {
                    return openAiJsonClient.completeJson(aiRequest);
                } catch (OpenAiJsonClientException retryEx) {
                    throw new PipelineTaskException(retryEx.getErrorCode().name(), "financial extraction failed", retryEx);
                }
            }
            throw new PipelineTaskException(ex.getErrorCode().name(), "financial extraction failed", ex);
        }
    }

    private void validateRequest(FinancialExtractionRequestDto request) {
        if (request == null || request.getJobId() == null || request.getJobId().isBlank()) {
            throw new PipelineTaskException("INVALID_INPUT", "jobId must be provided");
        }
        if (request.getDocType() == null || request.getDocType().isBlank()) {
            throw new PipelineTaskException("INVALID_INPUT", "docType must be provided");
        }
        if (request.getText() == null || request.getText().isBlank()) {
            throw new PipelineTaskException("INVALID_INPUT", "text must be provided");
        }
    }

    private boolean isSupportedFinancialDocType(String docType) {
        return Set.of("INVOICE", "RECEIPT", "BANK_STATEMENT", "BILL", "PURCHASE_ORDER").contains(docType);
    }

    private JsonNode loadSchema() {
        try {
            return objectMapper.readTree(financialSchemaResource.getInputStream());
        } catch (Exception ex) {
            throw new PipelineTaskException("INVALID_SCHEMA", "unable to load financial extraction schema", ex);
        }
    }

    private CanonicalFinancial mapCanonical(String requestDocType, JsonNode content) {
        try {
            JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(loadSchema());
            Set<com.networknt.schema.ValidationMessage> violations = schema.validate(content);
            if (!violations.isEmpty()) {
                throw new PipelineTaskException("INVALID_SCHEMA_OUTPUT", violations.iterator().next().getMessage());
            }

            String documentType = content.path("documentType").asText(requestDocType);
            String currency = content.path("currency").isNull() ? null : content.path("currency").asText();
            LocalDate periodStart = parseDate(content.path("periodStart").asText(null));
            LocalDate periodEnd = parseDate(content.path("periodEnd").asText(null));
            BigDecimal totalAmount = content.path("totalAmount").isNumber() ? content.path("totalAmount").decimalValue() : null;
            JsonNode lineItems = content.path("lineItems");
            BigDecimal confidence = content.path("confidence").decimalValue();

            ObjectNode provenance = objectMapper.createObjectNode();
            provenance.put("model", configuredModel);
            provenance.put("schemaVersion", SCHEMA_VERSION);

            ObjectNode canonical = objectMapper.createObjectNode();
            canonical.put("documentType", documentType);
            if (currency == null) {
                canonical.putNull("currency");
            } else {
                canonical.put("currency", currency);
            }
            canonical.put("periodStart", periodStart == null ? null : periodStart.toString());
            canonical.put("periodEnd", periodEnd == null ? null : periodEnd.toString());
            if (totalAmount == null) {
                canonical.putNull("totalAmount");
            } else {
                canonical.put("totalAmount", totalAmount);
            }
            canonical.set("lineItems", lineItems);
            canonical.put("confidence", confidence);
            canonical.set("provenance", provenance);

            return new CanonicalFinancial(documentType, currency, periodStart, periodEnd, totalAmount, canonical);
        } catch (PipelineTaskException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PipelineTaskException("INVALID_OUTPUT", "unable to map canonical financial data", ex);
        }
    }

    private String writeCanonicalJson(CanonicalFinancial canonical) {
        try {
            return objectMapper.writeValueAsString(canonical.json());
        } catch (Exception ex) {
            throw new PipelineTaskException("INVALID_OUTPUT", "unable to serialize canonical json", ex);
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new PipelineTaskException("UNEXPECTED_ERROR", "unable to hash input text", ex);
        }
    }

    private FinancialExtractionResultDto toResult(FinancialArtifactEntity entity) {
        return FinancialExtractionResultDto.builder()
                .artifactRef("fin:" + entity.getId())
                .documentType(entity.getDocumentType())
                .currency(entity.getCurrency())
                .totalAmount(entity.getTotalAmount())
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .build();
    }

    private record CanonicalFinancial(
            String documentType,
            String currency,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal totalAmount,
            JsonNode json
    ) {
    }
}
