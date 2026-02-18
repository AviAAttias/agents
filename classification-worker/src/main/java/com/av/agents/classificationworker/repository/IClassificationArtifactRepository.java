package com.av.agents.classificationworker.repository;

import com.av.agents.classificationworker.entity.ClassificationArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IClassificationArtifactRepository extends JpaRepository<ClassificationArtifactEntity, Long> {
}
