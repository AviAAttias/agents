package com.av.agents.sharedpersistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "approval_request", schema = "shared")
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
