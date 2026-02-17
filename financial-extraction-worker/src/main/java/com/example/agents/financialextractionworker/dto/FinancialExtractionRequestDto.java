package com.example.agents.financialextractionworker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FinancialExtractionRequestDto {
    @NotBlank
    String jobId;

    @NotBlank
    String docType;

    @NotBlank
    String text;

    @NotNull
    String taskType;

    String workflowId;
    String taskId;
}
