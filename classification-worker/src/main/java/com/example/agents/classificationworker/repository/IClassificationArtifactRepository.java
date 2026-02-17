package com.example.agents.classificationworker.repository;

import com.example.agents.classificationworker.entity.ClassificationArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IClassificationArtifactRepository extends JpaRepository<ClassificationArtifactEntity, Long> {
}
