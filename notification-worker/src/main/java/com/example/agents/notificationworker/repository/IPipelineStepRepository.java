package com.example.agents.notificationworker.repository;

import com.example.agents.notificationworker.entity.PipelineStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IPipelineStepRepository extends JpaRepository<PipelineStepEntity, Long> {
    Optional<PipelineStepEntity> findByIdempotencyKey(String idempotencyKey);
}
