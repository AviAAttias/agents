package com.av.agents.classificationworker.repository;

import com.av.agents.classificationworker.entity.TextArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ITextArtifactRepository extends JpaRepository<TextArtifactEntity, Long> {
}
