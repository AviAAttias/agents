package com.av.agents.textextractionworker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TextExtractionRequestDto {
    @NotBlank
    private String jobId;

    @NotBlank
    private String artifactRef;

    private String workflowId;
    private String taskId;
    private String taskType = "extract_text";
}
