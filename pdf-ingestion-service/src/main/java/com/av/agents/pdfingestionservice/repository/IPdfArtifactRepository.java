package com.av.agents.pdfingestionservice.repository;

import com.av.agents.pdfingestionservice.entity.PdfArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IPdfArtifactRepository extends JpaRepository<PdfArtifactEntity, Long> {
    Optional<PdfArtifactEntity> findBySha256(String sha256);
}
