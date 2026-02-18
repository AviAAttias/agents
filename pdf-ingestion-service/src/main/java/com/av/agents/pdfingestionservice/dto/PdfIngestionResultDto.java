package com.av.agents.pdfingestionservice.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PdfIngestionResultDto {
    String artifactRef;
    String sha256;
    long bytes;
    long durationMs;
}
