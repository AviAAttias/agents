package com.example.agents.pdfingestionservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pdf_artifact", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pdf_artifact_sha256", columnNames = {"sha256"})
})
@Getter
@Setter
public class PdfArtifactEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, length = 120)
    private String jobId;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(name = "content_length", nullable = false)
    private long contentLength;

    @Column(name = "storage_path", nullable = false, length = 1024)
    private String storagePath;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
