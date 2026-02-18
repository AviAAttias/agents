package com.av.agents.reconciliationworker.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReconciliationResultDto {
    String artifactRef;
    String validationStatus;
    int violationCount;
}
