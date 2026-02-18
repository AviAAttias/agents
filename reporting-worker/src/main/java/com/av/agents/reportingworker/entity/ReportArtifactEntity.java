package com.av.agents.reportingworker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "report_artifact", uniqueConstraints = {
        @UniqueConstraint(name = "uk_report_artifact_job_task", columnNames = {"job_id", "task_type"})
})
@Getter
@Setter
public class ReportArtifactEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(name = "format", nullable = false)
    private String format;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @Column(name = "artifact_ref")
    private String artifactRef;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
