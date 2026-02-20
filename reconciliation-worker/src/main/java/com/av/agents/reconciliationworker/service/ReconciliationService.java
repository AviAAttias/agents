package com.av.agents.reconciliationworker.service;

import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.reconciliationworker.dto.ReconciliationResultDto;
import com.av.agents.sharedpersistence.entity.ValidationArtifactEntity;
import com.av.agents.reconciliationworker.repository.IFinancialArtifactRepository;
import com.av.agents.sharedpersistence.repository.IValidationArtifactRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReconciliationService implements IReconciliationService {
    private static final String TASK_TYPE = "validate_reconcile";

    private final IFinancialArtifactRepository financialArtifactRepository;
    private final IValidationArtifactRepository validationArtifactRepository;
    private final ObjectMapper objectMapper;

    @Value("${reconciliation.tolerance.abs:0.01}")
    private BigDecimal toleranceAbs;

    @Override
    @Transactional
    public ReconciliationResultDto reconcile(String jobId, String financialArtifactRef) {
        if (jobId == null || jobId.isBlank()) {
            throw new PipelineTaskException("INVALID_INPUT", "jobId must be provided");
        }
        if (financialArtifactRef == null || financialArtifactRef.isBlank()) {
            throw new PipelineTaskException("INVALID_INPUT", "financialArtifact must be provided");
        }

        ValidationArtifactEntity existing = validationArtifactRepository.findByJobIdAndTaskType(jobId, TASK_TYPE).orElse(null);
        if (existing != null) {
            return toResult(existing);
        }

        long artifactId = parseFinancialArtifactId(financialArtifactRef);
        String canonicalJson = financialArtifactRepository.findCanonicalJsonById(artifactId)
                .orElseThrow(() -> new PipelineTaskException("NOT_FOUND", "financial artifact not found: " + financialArtifactRef));

        ValidationOutcome outcome = evaluateCanonicalJson(canonicalJson);

        ValidationArtifactEntity entity = new ValidationArtifactEntity();
        entity.setJobId(jobId);
        entity.setTaskType(TASK_TYPE);
        entity.setValidationJson(outcome.validationJson());
        entity.setValidationStatus(outcome.validationStatus());
        entity.setViolationCount(outcome.violationCount());
        entity.setCreatedAt(OffsetDateTime.now());

        ValidationArtifactEntity saved = validationArtifactRepository.save(entity);
        return toResult(saved);
    }

    private ValidationOutcome evaluateCanonicalJson(String canonicalJson) {
        try {
            JsonNode root = objectMapper.readTree(canonicalJson);
            String docType = root.path("documentType").asText("").toUpperCase(Locale.ROOT);

            ArrayNode violations = objectMapper.createArrayNode();
            BigDecimal totalAmount = decimalOrNull(root.path("totalAmount"));
            String currency = textOrNull(root.path("currency"));
            JsonNode lineItems = root.path("lineItems");

            BigDecimal reconciledTotal = BigDecimal.ZERO;
            if (lineItems.isArray()) {
                int idx = 0;
                for (JsonNode lineItem : lineItems) {
                    BigDecimal amount = decimalOrNull(lineItem.path("amount"));
                    if (amount != null) {
                        reconciledTotal = reconciledTotal.add(amount);
                    }
                    String lineCurrency = textOrNull(lineItem.path("currency"));
                    if (currency != null && lineCurrency != null && !currency.equalsIgnoreCase(lineCurrency)) {
                        violations.add(violation("CURRENCY_MISMATCH", "$.lineItems[" + idx + "].currency", "Line item currency differs from document currency"));
                    }
                    idx++;
                }
            }
            reconciledTotal = reconciledTotal.setScale(2, RoundingMode.HALF_UP);

            if (totalAmount != null) {
                BigDecimal delta = totalAmount.subtract(reconciledTotal).abs();
                if (delta.compareTo(toleranceAbs) > 0) {
                    violations.add(violation("TOTAL_MISMATCH", "$.totalAmount", "Sum of line items does not match total"));
                }
            }

            validateRequiredFields(root, docType, violations);
            validateDateRange(root, violations);
            validateNegativeTotals(docType, totalAmount, violations);

            String status = computeStatus(violations);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("validationStatus", status);
            result.set("violations", violations);
            if (reconciledTotal == null) {
                result.putNull("reconciledTotal");
            } else {
                result.put("reconciledTotal", reconciledTotal);
            }

            return new ValidationOutcome(objectMapper.writeValueAsString(result), status, violations.size());
        } catch (PipelineTaskException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PipelineTaskException("INVALID_INPUT", "unable to evaluate canonical json", ex);
        }
    }

    private void validateRequiredFields(JsonNode root, String docType, ArrayNode violations) {
        Map<String, List<String>> required = Map.of(
                "INVOICE", List.of("totalAmount", "currency", "lineItems", "periodStart", "periodEnd"),
                "RECEIPT", List.of("totalAmount", "currency", "lineItems"),
                "BANK_STATEMENT", List.of("currency", "lineItems", "periodStart", "periodEnd"),
                "BILL", List.of("totalAmount", "currency", "lineItems"),
                "PURCHASE_ORDER", List.of("totalAmount", "currency", "lineItems")
        );

        List<String> fields = required.get(docType);
        if (fields == null) {
            violations.add(violation("WARN_UNKNOWN_DOC_TYPE", "$.documentType", "Unknown documentType; required-field rules skipped"));
            return;
        }

        for (String field : fields) {
            JsonNode node = root.get(field);
            if (node == null || node.isNull() || (node.isTextual() && node.asText().isBlank()) || (node.isArray() && node.isEmpty())) {
                violations.add(violation("REQUIRED_FIELD_MISSING", "$." + field, "Required field missing for documentType " + docType));
            }
        }
    }

    private void validateDateRange(JsonNode root, ArrayNode violations) {
        LocalDate start = parseDate(textOrNull(root.path("periodStart")));
        LocalDate end = parseDate(textOrNull(root.path("periodEnd")));
        if (start != null && end != null && start.isAfter(end)) {
            violations.add(violation("INVALID_DATE_RANGE", "$.periodStart", "periodStart must be less than or equal to periodEnd"));
        }
    }

    private void validateNegativeTotals(String docType, BigDecimal totalAmount, ArrayNode violations) {
        if (totalAmount == null) {
            return;
        }
        Set<String> allowsNegative = Set.of("CREDIT_NOTE");
        if (totalAmount.signum() < 0 && !allowsNegative.contains(docType)) {
            violations.add(violation("NEGATIVE_TOTAL_NOT_ALLOWED", "$.totalAmount", "Negative totals are not allowed for documentType " + docType));
        }
    }

    private String computeStatus(ArrayNode violations) {
        if (violations.isEmpty()) {
            return "PASSED";
        }
        boolean warningOnly = true;
        for (JsonNode violation : violations) {
            if (!violation.path("code").asText().startsWith("WARN_")) {
                warningOnly = false;
                break;
            }
        }
        return warningOnly ? "WARN" : "FAILED";
    }

    private ObjectNode violation(String code, String path, String message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("code", code);
        node.put("path", path);
        node.put("message", message);
        return node;
    }

    private ReconciliationResultDto toResult(ValidationArtifactEntity entity) {
        return ReconciliationResultDto.builder()
                .artifactRef("val:" + entity.getId())
                .validationStatus(entity.getValidationStatus())
                .violationCount(entity.getViolationCount())
                .build();
    }

    private long parseFinancialArtifactId(String financialArtifactRef) {
        if (!financialArtifactRef.startsWith("fin:")) {
            throw new PipelineTaskException("INVALID_INPUT", "financialArtifact must match format fin:{id}");
        }
        try {
            return Long.parseLong(financialArtifactRef.substring(4));
        } catch (NumberFormatException ex) {
            throw new PipelineTaskException("INVALID_INPUT", "financialArtifact id is invalid", ex);
        }
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.decimalValue();
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return LocalDate.parse(raw);
    }

    private record ValidationOutcome(String validationJson, String validationStatus, int violationCount) {}
}
