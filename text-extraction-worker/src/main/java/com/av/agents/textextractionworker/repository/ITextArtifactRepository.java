package com.av.agents.textextractionworker.repository;

import com.av.agents.textextractionworker.entity.TextArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ITextArtifactRepository extends JpaRepository<TextArtifactEntity, Long> {
}
