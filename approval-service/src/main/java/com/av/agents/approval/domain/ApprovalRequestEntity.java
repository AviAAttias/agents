package com.av.agents.approval.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "approval_request")
@Getter
@Setter
public class ApprovalRequestEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "job_id", nullable = false, unique = true)
  private String jobId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ApprovalStatus status;

  @Column(name = "report_artifact_ref")
  private String reportArtifactRef;

  @Column(name = "reviewer_email")
  private String reviewerEmail;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_comment")
  private String decisionComment;

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public String artifactRef() {
    return id == null ? null : "appr:" + id;
  }
}
