package com.example.agents.reportingworker.repository;

import com.example.agents.reportingworker.entity.ReportArtifactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportArtifactRepository extends JpaRepository<ReportArtifactEntity, Long> {
    Optional<ReportArtifactEntity> findByJobIdAndTaskType(String jobId, String taskType);
}
