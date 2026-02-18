package com.av.agents.approval.facade;

import com.av.agents.sharedpersistence.entity.ApprovalRequestEntity;
import com.av.agents.approval.service.ApprovalDecision;
import com.av.agents.approval.service.CreateApprovalRequest;
import com.av.agents.approval.service.DecisionResult;
import com.av.agents.approval.service.IApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalFacade {

  private final IApprovalService approvalService;

  public ApprovalRequestEntity createRequest(CreateApprovalRequest request) {
    return approvalService.createApprovalRequest(request);
  }

  public DecisionResult decide(String jobId, ApprovalDecision decision, String reviewer, String comment) {
    return approvalService.decide(jobId, decision, reviewer, comment);
  }
}
