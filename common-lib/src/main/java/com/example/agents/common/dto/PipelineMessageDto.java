package com.example.agents.common.dto;

import com.example.agents.common.enums.PipelineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
@Schema(description = "Generic payload shared between pipeline services")
public class PipelineMessageDto {
    private String jobId;
    private String taskType;
    private PipelineStatus status;
    private String artifactRef;
    private String payloadJson;
    private OffsetDateTime processedAt;
}
