package com.av.agents.notificationworker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PipelineStepRequestDto {
    @NotBlank
    private String jobId;
    @NotBlank
    private String taskType;
    @NotBlank
    private String recipient;
    @NotBlank
    private String reportArtifact;
    private String payloadJson;
}
