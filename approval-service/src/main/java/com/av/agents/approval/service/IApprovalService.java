package com.av.agents.approval.service;

import com.av.agents.approval.domain.ApprovalRequestEntity;

public interface IApprovalService {

  ApprovalRequestEntity createApprovalRequest(CreateApprovalRequest request);

  DecisionResult decide(String jobId, ApprovalDecision decision, String reviewer, String comment);
}
