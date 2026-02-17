package com.example.agents.reconciliationworker.facade;

import com.example.agents.reconciliationworker.dto.ReconciliationResultDto;
import com.example.agents.reconciliationworker.service.IReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReconciliationFacade {
    private final IReconciliationService reconciliationService;

    public ReconciliationResultDto reconcile(String jobId, String financialArtifactRef) {
        return reconciliationService.reconcile(jobId, financialArtifactRef);
    }
}
