package com.av.agents.financialextractionworker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "financial_artifact", uniqueConstraints = {
        @UniqueConstraint(name = "uk_financial_artifact_job_task", columnNames = {"job_id", "task_type"})
})
@Getter
@Setter
public class FinancialArtifactEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 120)
    private String jobId;

    @Column(name = "task_type", nullable = false, length = 120)
    private String taskType;

    @Column(name = "canonical_json", nullable = false, columnDefinition = "json")
    private String canonicalJson;

    @Column(name = "document_type", nullable = false, length = 80)
    private String documentType;

    @Column(name = "currency", length = 16)
    private String currency;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "input_text_sha256", nullable = false, length = 64)
    private String inputTextSha256;

    @Column(name = "input_char_count", nullable = false)
    private int inputCharCount;

    @Column(name = "was_truncated", nullable = false)
    private boolean wasTruncated;

    @Column(name = "model", nullable = false, length = 120)
    private String model;

    @Column(name = "schema_version", nullable = false, length = 32)
    private String schemaVersion;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
