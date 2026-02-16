package com.example.agents.textextractionworker.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TextExtractionResultDto {
    String text;
    String artifactRef;
    String textArtifact;
    long inputBytes;
    int outputChars;
    long durationMs;
    boolean wasTruncated;
    int pageCount;
    String extractionMethod;
    String sha256;
}
