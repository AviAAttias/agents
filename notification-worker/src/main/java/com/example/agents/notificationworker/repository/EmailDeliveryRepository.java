package com.example.agents.notificationworker.repository;

import com.example.agents.notificationworker.entity.EmailDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailDeliveryRepository extends JpaRepository<EmailDeliveryEntity, Long> {
    Optional<EmailDeliveryEntity> findByJobIdAndRecipientAndReportArtifactRef(String jobId, String recipient, String reportArtifactRef);
}
