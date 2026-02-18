package com.av.agents.reconciliationworker.service;

import com.av.agents.reconciliationworker.dto.ReconciliationResultDto;

public interface IReconciliationService {
    ReconciliationResultDto reconcile(String jobId, String financialArtifactRef);
}
