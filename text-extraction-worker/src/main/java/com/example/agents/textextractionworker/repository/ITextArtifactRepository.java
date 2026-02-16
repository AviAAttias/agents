package com.example.agents.textextractionworker.repository;

import com.example.agents.textextractionworker.entity.TextArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ITextArtifactRepository extends JpaRepository<TextArtifactEntity, Long> {
}
