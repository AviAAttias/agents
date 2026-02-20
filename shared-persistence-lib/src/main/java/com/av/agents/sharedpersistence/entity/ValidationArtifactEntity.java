package com.av.agents.sharedpersistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "validation_artifact", uniqueConstraints = @UniqueConstraint(name = "uk_validation_artifact_job_task", columnNames = {"job_id", "task_type"}))
@Getter
@Setter
public class ValidationArtifactEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "job_id", nullable = false, length = 120)
  private String jobId;
  @Column(name = "task_type", nullable = false, length = 120)
  private String taskType;
  @Column(name = "validation_json", nullable = false, columnDefinition = "json")
  private String validationJson;
  @Column(name = "validation_status", nullable = false, length = 16)
  private String validationStatus;
  @Column(name = "violation_count", nullable = false)
  private int violationCount;
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
