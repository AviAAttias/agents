package com.av.agents.reconciliationworker.repository;

import com.av.agents.reconciliationworker.entity.PipelineStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IPipelineStepRepository extends JpaRepository<PipelineStepEntity, Long> {
    Optional<PipelineStepEntity> findByIdempotencyKey(String idempotencyKey);
}
