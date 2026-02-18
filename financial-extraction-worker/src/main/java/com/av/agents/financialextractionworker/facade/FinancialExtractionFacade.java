package com.av.agents.financialextractionworker.facade;

import com.av.agents.financialextractionworker.dto.FinancialExtractionRequestDto;
import com.av.agents.financialextractionworker.dto.FinancialExtractionResultDto;
import com.av.agents.financialextractionworker.service.IFinancialExtractionService;
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
