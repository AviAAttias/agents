package com.example.agents.reconciliationworker.service;

import com.example.agents.reconciliationworker.dto.ReconciliationResultDto;

public interface IReconciliationService {
    ReconciliationResultDto reconcile(String jobId, String financialArtifactRef);
}
