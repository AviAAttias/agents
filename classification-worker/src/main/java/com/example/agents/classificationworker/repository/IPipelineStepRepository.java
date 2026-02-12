package com.example.agents.classificationworker.repository;

import com.example.agents.classificationworker.entity.PipelineStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IPipelineStepRepository extends JpaRepository<PipelineStepEntity, Long> {
    Optional<PipelineStepEntity> findByIdempotencyKey(String idempotencyKey);
}
