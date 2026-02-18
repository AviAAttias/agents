package com.av.agents.approval.service;

import com.av.agents.approval.domain.ApprovalRequestEntity;
import com.av.agents.approval.domain.ApprovalStatus;
import com.av.agents.approval.infra.ConductorEventPublisher;
import com.av.agents.approval.repository.ApprovalRequestRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApprovalService implements IApprovalService {

  private final ApprovalRequestRepository repository;
  private final ConductorEventPublisher eventPublisher;
  private final Clock clock;

  @Override
  @Transactional
  public ApprovalRequestEntity createApprovalRequest(CreateApprovalRequest request) {
    ApprovalRequestEntity entity = new ApprovalRequestEntity();
    entity.setJobId(request.jobId());
    entity.setStatus(ApprovalStatus.PENDING);
    entity.setReportArtifactRef(request.reportArtifactRef());
    entity.setReviewerEmail(request.reviewerEmail());
    entity.setExpiresAt(request.expiresAt());

    try {
      ApprovalRequestEntity created = repository.saveAndFlush(entity);
      created.setReportArtifactRef(created.artifactRef());
      return repository.save(created);
    } catch (DataIntegrityViolationException ex) {
      throw new InvalidApprovalStateException("Approval request already exists for jobId=" + request.jobId());
    }
  }

  @Override
  @Transactional
  public DecisionResult decide(String jobId, ApprovalDecision decision, String reviewer, String comment) {
    ApprovalRequestEntity entity = repository.findByJobId(jobId).orElseThrow(() -> new ApprovalNotFoundException(jobId));

    if (entity.getStatus() != ApprovalStatus.PENDING) {
      throw new InvalidApprovalStateException("Approval request already decided for jobId=" + jobId);
    }

    Instant now = Instant.now(clock);
    if (entity.getExpiresAt() != null && now.isAfter(entity.getExpiresAt())) {
      throw new InvalidApprovalStateException("Approval request expired for jobId=" + jobId);
    }

    entity.setStatus(decision.toStatus());
    entity.setReviewerEmail(reviewer);
    entity.setDecisionComment(comment);
    entity.setDecidedAt(now);
    repository.save(entity);

    Map<String, Object> payload = new HashMap<>();
    payload.put("decision", decision.name());
    payload.put("reviewer", reviewer);
    payload.put("comment", comment);
    payload.put("decidedAt", now.toString());
    eventPublisher.publish("approval." + jobId, payload);

    return new DecisionResult(decision.name(), reviewer, comment, now);
  }
}
