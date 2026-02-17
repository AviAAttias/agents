package com.example.agents.classificationworker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "classification_artifact")
@Getter
@Setter
public class ClassificationArtifactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 120)
    private String jobId;

    @Column(name = "task_type", nullable = false, length = 120)
    private String taskType;

    // FIX: DB column is VARCHAR/TEXT, not CLOB. Map as TEXT to satisfy schema validation.
    @Column(name = "raw_response_json", nullable = false, columnDefinition = "TEXT")
    private String rawResponseJson;

    // FIX: same here.
    @Column(name = "mapped_result_json", nullable = false, columnDefinition = "TEXT")
    private String mappedResultJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
