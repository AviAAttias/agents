package com.av.agents.notificationworker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "email_delivery", uniqueConstraints = {
        @UniqueConstraint(name = "uk_email_delivery_job_recipient_artifact", columnNames = {"job_id", "recipient", "report_artifact_ref"})
})
@Getter
@Setter
public class EmailDeliveryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "report_artifact_ref", nullable = false)
    private String reportArtifactRef;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
