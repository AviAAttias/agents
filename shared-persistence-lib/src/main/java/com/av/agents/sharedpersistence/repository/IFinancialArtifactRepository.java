package com.av.agents.sharedpersistence.repository;

import com.av.agents.sharedpersistence.entity.FinancialArtifactEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IFinancialArtifactRepository extends JpaRepository<FinancialArtifactEntity, Long> {
  Optional<FinancialArtifactEntity> findByJobIdAndTaskType(String jobId, String taskType);
}
