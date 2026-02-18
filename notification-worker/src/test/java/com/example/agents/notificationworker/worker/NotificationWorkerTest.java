package com.example.agents.notificationworker.worker;

import com.example.agents.common.enums.PipelineStatus;
import com.example.agents.notificationworker.dto.PipelineStepRequestDto;
import com.example.agents.notificationworker.entity.EmailDeliveryEntity;
import com.example.agents.notificationworker.repository.EmailDeliveryRepository;
import com.example.agents.notificationworker.service.INotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationWorkerTest {

    @Mock
    private EmailDeliveryRepository emailDeliveryRepository;
    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private NotificationWorker notificationWorker = new NotificationWorker(emailDeliveryRepository, notificationService, new ObjectMapper());

    @Test
    void process_preventsDuplicateResend() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-1");
        request.setTaskType("send_email");
        request.setRecipient("reviewer@example.com");
        request.setReportArtifact("rep:1");

        EmailDeliveryEntity existing = new EmailDeliveryEntity();
        existing.setId(21L);
        when(emailDeliveryRepository.findByJobIdAndRecipientAndReportArtifactRef("job-1", "reviewer@example.com", "rep:1"))
                .thenReturn(Optional.of(existing));

        var response = notificationWorker.process(request);

        assertThat(response.getStatus()).isEqualTo(PipelineStatus.PROCESSED);
        assertThat(response.getPayloadJson()).contains("\"status\":\"SKIPPED_DUPLICATE\"");
        verify(notificationService, never()).send(any(), any(), any());
        verify(emailDeliveryRepository, never()).save(any());
    }

    @Test
    void process_rendersTemplateWithSummaryValidationAndLink() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-2");
        request.setTaskType("send_email");
        request.setRecipient("finance@example.com");
        request.setReportArtifact("https://reports.example.com/job-2");
        request.setPayloadJson("{\"summaryTotals\":\"$1200\",\"validationStatus\":\"PASSED\"}");

        when(emailDeliveryRepository.findByJobIdAndRecipientAndReportArtifactRef(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(notificationService.send(any(), any(), any())).thenReturn("smtp-1");
        when(emailDeliveryRepository.save(any())).thenAnswer(invocation -> {
            EmailDeliveryEntity entity = invocation.getArgument(0);
            entity.setId(7L);
            return entity;
        });

        notificationWorker.process(request);

        var bodyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(notificationService).send(eq("finance@example.com"), eq("Financial Report Approved - Job job-2"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("Summary Totals: $1200");
        assertThat(bodyCaptor.getValue()).contains("Validation Status: PASSED");
        assertThat(bodyCaptor.getValue()).contains("Report Link: https://reports.example.com/job-2");
    }
}
