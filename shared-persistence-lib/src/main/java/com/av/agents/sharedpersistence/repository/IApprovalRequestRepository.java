package com.av.agents.sharedpersistence.repository;

import com.av.agents.sharedpersistence.entity.ApprovalRequestEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, Long> {
  Optional<ApprovalRequestEntity> findByJobId(String jobId);
}
