package com.av.agents.reconciliationworker.repository;

import com.av.agents.reconciliationworker.entity.ValidationArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValidationArtifactRepository extends JpaRepository<ValidationArtifactEntity, Long> {
    Optional<ValidationArtifactEntity> findByJobIdAndTaskType(String jobId, String taskType);
}
