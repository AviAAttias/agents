package com.example.agents.classificationworker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "classification_pipeline_step", uniqueConstraints = {
        @UniqueConstraint(name = "uk_classification_pipeline_step_job_task", columnNames = {"job_id", "task_type"}),
        @UniqueConstraint(name = "uk_classification_pipeline_step_idempotency", columnNames = {"idempotency_key"})
})
@Getter
@Setter
public class PipelineStepEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "artifact_ref")
    private String artifactRef;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
