package com.example.agents.classificationworker.repository;

import com.example.agents.classificationworker.entity.TextArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ITextArtifactRepository extends JpaRepository<TextArtifactEntity, Long> {
}
