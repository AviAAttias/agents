package com.av.agents.sharedpersistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "text_artifact")
@Getter
@Setter
public class TextArtifactEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "job_id", nullable = false, length = 120)
  private String jobId;

  @Lob
  @Column(name = "text_body", nullable = false)
  private String textBody;

  @Column(name = "was_truncated", nullable = false)
  private boolean wasTruncated;

  @Column(name = "page_count", nullable = false)
  private int pageCount;

  @Column(name = "input_bytes", nullable = false)
  private long inputBytes;

  @Column(name = "output_chars", nullable = false)
  private int outputChars;

  @Column(name = "sha256", nullable = false, length = 64)
  private String sha256;

  @Column(name = "extraction_method", nullable = false, length = 64)
  private String extractionMethod;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
}
