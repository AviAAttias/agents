package com.example.agents.approval.repository;

import com.example.agents.approval.domain.ApprovalRequestEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, Long> {

  Optional<ApprovalRequestEntity> findByJobId(String jobId);
}
