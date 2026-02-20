package com.av.agents.sharedpersistence.repository;

import com.av.agents.sharedpersistence.entity.ValidationArtifactEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IValidationArtifactRepository extends JpaRepository<ValidationArtifactEntity, Long> {
  Optional<ValidationArtifactEntity> findByJobIdAndTaskType(String jobId, String taskType);
}
