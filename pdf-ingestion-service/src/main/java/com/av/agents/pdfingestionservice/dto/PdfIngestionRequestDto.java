package com.av.agents.pdfingestionservice.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PdfIngestionRequestDto {
    String jobId;
    String pdfUrl;
    String workflowId;
    String taskId;
    String taskType;
}
