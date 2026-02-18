package com.av.agents.reconciliationworker.facade;

import com.av.agents.reconciliationworker.dto.ReconciliationResultDto;
import com.av.agents.reconciliationworker.service.IReconciliationService;
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
