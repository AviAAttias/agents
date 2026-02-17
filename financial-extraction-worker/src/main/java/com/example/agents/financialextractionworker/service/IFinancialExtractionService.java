package com.example.agents.financialextractionworker.service;

import com.example.agents.financialextractionworker.dto.FinancialExtractionRequestDto;
import com.example.agents.financialextractionworker.dto.FinancialExtractionResultDto;

public interface IFinancialExtractionService {
    FinancialExtractionResultDto extract(FinancialExtractionRequestDto request);
}
