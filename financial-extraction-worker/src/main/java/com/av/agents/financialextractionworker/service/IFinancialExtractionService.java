package com.av.agents.financialextractionworker.service;

import com.av.agents.financialextractionworker.dto.FinancialExtractionRequestDto;
import com.av.agents.financialextractionworker.dto.FinancialExtractionResultDto;

public interface IFinancialExtractionService {
    FinancialExtractionResultDto extract(FinancialExtractionRequestDto request);
}
