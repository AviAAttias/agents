package com.av.agents.sharedpersistence.repository;

import com.av.agents.sharedpersistence.entity.ReportArtifactEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IReportArtifactRepository extends JpaRepository<ReportArtifactEntity, Long> {
  Optional<ReportArtifactEntity> findByJobIdAndTaskType(String jobId, String taskType);
}
