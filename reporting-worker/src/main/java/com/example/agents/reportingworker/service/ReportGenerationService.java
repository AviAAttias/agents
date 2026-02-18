package com.example.agents.reportingworker.service;

import com.example.agents.reportingworker.dto.PipelineStepRequestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportGenerationService implements IReportGenerationService {
    private final ObjectMapper objectMapper;

    @Override
    public String generateMarkdownReport(PipelineStepRequestDto requestDto) {
        JsonNode payload = parsePayload(requestDto.getPayloadJson());
        String documentType = textOrDefault(payload, "documentType", "RECONCILIATION");
        String validationArtifactRef = textOrDefault(payload, "validationArtifact", "");
        String validationStatus = textOrDefault(payload, "validationStatus", "UNKNOWN");

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Report\n\n");
        markdown.append("## Header\n");
        markdown.append("- jobId: ").append(requestDto.getJobId()).append("\n");
        markdown.append("- documentType: ").append(documentType).append("\n\n");

        markdown.append("## Financial summary\n");
        appendFinancialSummary(markdown, payload.path("financialSummary"));

        markdown.append("\n## Validation status\n");
        markdown.append("- status: ").append(validationStatus).append("\n");
        markdown.append("- validationArtifact: ").append(validationArtifactRef).append("\n\n");

        markdown.append("## Violations table\n");
        appendViolations(markdown, payload.path("violations"));

        markdown.append("\n## Reviewer checklist\n");
        appendChecklist(markdown, payload.path("reviewerChecklist"));

        return markdown.toString();
    }

    private JsonNode parsePayload(String payloadJson) {
        try {
            return objectMapper.readTree(payloadJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("payloadJson must be valid JSON", e);
        }
    }

    private String textOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? defaultValue : value.asText();
    }

    private void appendFinancialSummary(StringBuilder markdown, JsonNode financialSummary) {
        if (!financialSummary.isObject() || !financialSummary.fieldNames().hasNext()) {
            markdown.append("- No financial summary provided.\n");
            return;
        }

        List<String> keys = new ArrayList<>();
        Iterator<String> fieldNames = financialSummary.fieldNames();
        while (fieldNames.hasNext()) {
            keys.add(fieldNames.next());
        }
        keys.sort(String::compareTo);

        for (String key : keys) {
            markdown.append("- ").append(key).append(": ")
                    .append(normalizeNode(financialSummary.get(key))).append("\n");
        }
    }

    private void appendViolations(StringBuilder markdown, JsonNode violations) {
        markdown.append("| Code | Severity | Message | Path |\n");
        markdown.append("| --- | --- | --- | --- |\n");
        if (!violations.isArray() || violations.isEmpty()) {
            markdown.append("| NONE | INFO | No violations found | - |\n");
            return;
        }

        List<Map<String, String>> orderedViolations = new ArrayList<>();
        for (JsonNode violation : violations) {
            orderedViolations.add(Map.of(
                    "code", textOrDefault(violation, "code", "UNKNOWN"),
                    "severity", textOrDefault(violation, "severity", "UNKNOWN"),
                    "message", textOrDefault(violation, "message", ""),
                    "path", textOrDefault(violation, "path", "")
            ));
        }

        orderedViolations.stream()
                .sorted(Comparator
                        .comparing((Map<String, String> row) -> row.get("code"))
                        .thenComparing(row -> row.get("severity"))
                        .thenComparing(row -> row.get("message"))
                        .thenComparing(row -> row.get("path")))
                .forEach(row -> markdown.append("| ")
                        .append(row.get("code")).append(" | ")
                        .append(row.get("severity")).append(" | ")
                        .append(row.get("message")).append(" | ")
                        .append(row.get("path")).append(" |\n"));
    }

    private void appendChecklist(StringBuilder markdown, JsonNode checklist) {
        if (!checklist.isArray() || checklist.isEmpty()) {
            markdown.append("- [ ] Confirm financial totals\n")
                    .append("- [ ] Validate all critical violations\n")
                    .append("- [ ] Approve report distribution\n");
            return;
        }

        List<String> items = new ArrayList<>();
        for (JsonNode item : checklist) {
            String value = normalizeNode(item);
            if (!value.isBlank()) {
                items.add(value);
            }
        }
        if (items.isEmpty()) {
            markdown.append("- [ ] Confirm financial totals\n")
                    .append("- [ ] Validate all critical violations\n")
                    .append("- [ ] Approve report distribution\n");
            return;
        }

        for (String item : items.stream().sorted(String::compareTo).collect(Collectors.toList())) {
            markdown.append("- [ ] ").append(item).append("\n");
        }
    }

    private String normalizeNode(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }
        if (value.isValueNode()) {
            return value.asText();
        }
        return value.toString().toUpperCase(Locale.ROOT);
    }
}
