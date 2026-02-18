package com.example.agents.notificationworker.worker;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.common.enums.PipelineStatus;
import com.example.agents.notificationworker.dto.PipelineStepRequestDto;
import com.example.agents.notificationworker.entity.EmailDeliveryEntity;
import com.example.agents.notificationworker.repository.EmailDeliveryRepository;
import com.example.agents.notificationworker.service.INotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private final EmailDeliveryRepository emailDeliveryRepository;
    private final INotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PipelineMessageDto process(PipelineStepRequestDto request) {
        long startedAt = System.currentTimeMillis();

        EmailDeliveryEntity existing = emailDeliveryRepository
                .findByJobIdAndRecipientAndReportArtifactRef(request.getJobId(), request.getRecipient(), request.getReportArtifact())
                .orElse(null);

        if (existing != null) {
            return buildResponse(request, existing.getId(), "SKIPPED_DUPLICATE", startedAt, PipelineStatus.PROCESSED);
        }

        EmailDeliveryEntity entity = new EmailDeliveryEntity();
        entity.setJobId(request.getJobId());
        entity.setRecipient(request.getRecipient());
        entity.setReportArtifactRef(request.getReportArtifact());
        entity.setCreatedAt(OffsetDateTime.now());

        try {
            String providerMessageId = notificationService.send(
                    request.getRecipient(),
                    "Financial Report Approved - Job " + request.getJobId(),
                    buildEmailBody(request)
            );
            entity.setStatus("SENT");
            entity.setProviderMessageId(providerMessageId);
            EmailDeliveryEntity saved = emailDeliveryRepository.save(entity);
            return buildResponse(request, saved.getId(), "SENT", startedAt, PipelineStatus.PROCESSED);
        } catch (MailException ex) {
            entity.setStatus("FAILED");
            EmailDeliveryEntity saved = emailDeliveryRepository.save(entity);
            return buildResponse(request, saved.getId(), "FAILED", startedAt, PipelineStatus.FAILED);
        }
    }

    private String buildEmailBody(PipelineStepRequestDto request) {
        String summaryTotals = "N/A";
        String validationStatus = "N/A";

        if (request.getPayloadJson() != null && !request.getPayloadJson().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(request.getPayloadJson());
                summaryTotals = root.path("summaryTotals").asText(summaryTotals);
                validationStatus = root.path("validationStatus").asText(validationStatus);
            } catch (Exception ignored) {
                // keep defaults if optional payload shape is absent
            }
        }

        return "Summary Totals: " + summaryTotals + "\n"
                + "Validation Status: " + validationStatus + "\n"
                + "Report Link: " + request.getReportArtifact();
    }

    private PipelineMessageDto buildResponse(
            PipelineStepRequestDto request,
            Long deliveryId,
            String status,
            long startedAt,
            PipelineStatus pipelineStatus
    ) {
        long durationMs = System.currentTimeMillis() - startedAt;
        String payload = String.format(
                "{\"deliveryId\":%d,\"status\":\"%s\",\"durationMs\":%d}",
                deliveryId,
                status,
                durationMs
        );

        return PipelineMessageDto.builder()
                .jobId(request.getJobId())
                .taskType(request.getTaskType())
                .status(pipelineStatus)
                .artifactRef(request.getReportArtifact())
                .payloadJson(payload)
                .processedAt(OffsetDateTime.now())
                .build();
    }
}
