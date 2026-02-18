package com.av.agents.approval.worker;

import com.av.agents.approval.facade.ApprovalFacade;
import com.av.agents.approval.service.CreateApprovalRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalWorker {

  private final ApprovalFacade facade;

  public Map<String, Object> requestApproval(String jobId, String reviewerEmail, String reportArtifactRef) {
    var entity = facade.createRequest(
        new CreateApprovalRequest(jobId, reportArtifactRef, reviewerEmail, Instant.now().plus(24, ChronoUnit.HOURS)));

    return Map.of(
        "approvalRequestId", entity.getId(),
        "artifactRef", entity.artifactRef(),
        "status", entity.getStatus().name());
  }
}
