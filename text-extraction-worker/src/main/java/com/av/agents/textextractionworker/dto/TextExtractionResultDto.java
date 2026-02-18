package com.av.agents.textextractionworker.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
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
