package com.av.agents.financialextractionworker.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class FinancialExtractionResultDto {
    String artifactRef;
    String documentType;
    String currency;
    BigDecimal totalAmount;
    LocalDate periodStart;
    LocalDate periodEnd;
}
