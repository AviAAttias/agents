package com.av.agents.reportingworker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pipeline_step", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pipeline_step_job_task", columnNames = {"job_id", "task_type"}),
        @UniqueConstraint(name = "uk_pipeline_step_idempotency", columnNames = {"idempotency_key"})
})
@Getter
@Setter
public class PipelineStepEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "job_id", nullable = false)
    private String jobId;
    @Column(name = "task_type", nullable = false)
    private String taskType;
    @Column(name = "status", nullable = false)
    private String status;
    @Column(name = "artifact_ref")
    private String artifactRef;
    @Lob
    @Column(name = "payload_json")
    private String payloadJson;
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
