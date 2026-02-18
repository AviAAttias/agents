package com.av.agents.financialextractionworker.repository;

import com.av.agents.financialextractionworker.entity.FinancialArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinancialArtifactRepository extends JpaRepository<FinancialArtifactEntity, Long> {
    Optional<FinancialArtifactEntity> findByJobIdAndTaskType(String jobId, String taskType);
}
