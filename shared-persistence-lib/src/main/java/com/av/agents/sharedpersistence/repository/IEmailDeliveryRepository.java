package com.av.agents.sharedpersistence.repository;

import com.av.agents.sharedpersistence.entity.EmailDeliveryEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IEmailDeliveryRepository extends JpaRepository<EmailDeliveryEntity, Long> {
  Optional<EmailDeliveryEntity> findByJobIdAndRecipientAndReportArtifactRef(String jobId, String recipient, String reportArtifactRef);
}
