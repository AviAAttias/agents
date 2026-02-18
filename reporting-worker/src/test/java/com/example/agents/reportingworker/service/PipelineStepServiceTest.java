package com.example.agents.reportingworker.service;

import com.example.agents.reportingworker.dto.PipelineStepRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineStepServiceTest {

    private final ReportGenerationService service = new ReportGenerationService(new ObjectMapper());

    @Test
    void generateMarkdownReport_containsRequiredSections() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-1");
        request.setTaskType("generate_report");
        request.setPayloadJson("""
                {
                  "documentType": "RECON",
                  "validationArtifact": "val:100",
                  "validationStatus": "PASSED",
                  "financialSummary": {"total": "120.00"},
                  "violations": [{"code": "V-1", "severity": "HIGH", "message": "Mismatch", "path": "lineItems[0]"}],
                  "reviewerChecklist": ["Confirm totals"]
                }
                """);

        String report = service.generateMarkdownReport(request);

        assertThat(report).contains("## Header");
        assertThat(report).contains("## Financial summary");
        assertThat(report).contains("## Validation status");
        assertThat(report).contains("## Violations table");
        assertThat(report).contains("## Reviewer checklist");
    }

    @Test
    void generateMarkdownReport_isDeterministic() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-2");
        request.setTaskType("generate_report");
        request.setPayloadJson("""
                {
                  "documentType": "RECON",
                  "validationArtifact": "val:101",
                  "validationStatus": "FAILED",
                  "financialSummary": {"b": "2", "a": "1"},
                  "violations": [
                    {"code": "V-2", "severity": "LOW", "message": "Late", "path": "lineItems[3]"},
                    {"code": "V-1", "severity": "HIGH", "message": "Mismatch", "path": "lineItems[0]"}
                  ],
                  "reviewerChecklist": ["b-item", "a-item"]
                }
                """);

        String report1 = service.generateMarkdownReport(request);
        String report2 = service.generateMarkdownReport(request);

        assertThat(report1).isEqualTo(report2);
    }
}
