package com.example.agents.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.agents.approval.domain.ApprovalRequestEntity;
import com.example.agents.approval.domain.ApprovalStatus;
import com.example.agents.approval.infra.ConductorEventPublisher;
import com.example.agents.approval.repository.ApprovalRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

  @Mock private ApprovalRequestRepository repository;
  @Mock private ConductorEventPublisher eventPublisher;
  @Captor private ArgumentCaptor<Map<String, Object>> payloadCaptor;

  private ApprovalService approvalService;

  @BeforeEach
  void setUp() {
    Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T10:15:30Z"), ZoneOffset.UTC);
    approvalService = new ApprovalService(repository, eventPublisher, fixedClock);
  }

  @Test
  void decide_shouldTransitionPendingToApproved_andPersist() {
    ApprovalRequestEntity pending = new ApprovalRequestEntity();
    pending.setJobId("job-1");
    pending.setStatus(ApprovalStatus.PENDING);
    pending.setExpiresAt(Instant.parse("2026-01-01T11:15:30Z"));
    when(repository.findByJobId("job-1")).thenReturn(Optional.of(pending));

    DecisionResult result = approvalService.decide("job-1", ApprovalDecision.APPROVED, "reviewer@example.com", "ok");

    assertThat(result.decision()).isEqualTo("APPROVED");
    assertThat(pending.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
    assertThat(pending.getDecidedAt()).isEqualTo(Instant.parse("2026-01-01T10:15:30Z"));
    verify(repository).save(pending);
  }

  @Test
  void decide_shouldRejectExpiredRequest() {
    ApprovalRequestEntity pending = new ApprovalRequestEntity();
    pending.setJobId("job-2");
    pending.setStatus(ApprovalStatus.PENDING);
    pending.setExpiresAt(Instant.parse("2026-01-01T10:10:30Z"));
    when(repository.findByJobId("job-2")).thenReturn(Optional.of(pending));

    assertThatThrownBy(() -> approvalService.decide("job-2", ApprovalDecision.REJECTED, "reviewer@example.com", "late"))
        .isInstanceOf(InvalidApprovalStateException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void decide_shouldPublishContractPayload_withDecisionField() {
    ApprovalRequestEntity pending = new ApprovalRequestEntity();
    pending.setJobId("job-3");
    pending.setStatus(ApprovalStatus.PENDING);
    pending.setExpiresAt(Instant.parse("2026-01-01T11:15:30Z"));
    when(repository.findByJobId("job-3")).thenReturn(Optional.of(pending));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    approvalService.decide("job-3", ApprovalDecision.APPROVED, "reviewer@example.com", "ship it");

    verify(eventPublisher).publish(eq("approval.job-3"), payloadCaptor.capture());
    Map<String, Object> payload = payloadCaptor.getValue();
    assertThat(payload)
        .containsEntry("decision", "APPROVED")
        .containsEntry("reviewer", "reviewer@example.com")
        .containsEntry("comment", "ship it")
        .containsKey("decidedAt");
  }
}
