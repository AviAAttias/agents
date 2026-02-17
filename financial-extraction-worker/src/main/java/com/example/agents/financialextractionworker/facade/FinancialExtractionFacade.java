package com.example.agents.financialextractionworker.facade;

import com.example.agents.financialextractionworker.dto.FinancialExtractionRequestDto;
import com.example.agents.financialextractionworker.dto.FinancialExtractionResultDto;
import com.example.agents.financialextractionworker.service.IFinancialExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinancialExtractionFacade {
    private final IFinancialExtractionService financialExtractionService;

    public FinancialExtractionResultDto extract(FinancialExtractionRequestDto request) {
        return financialExtractionService.extract(request);
    }
}
