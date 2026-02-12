package com.example.agents.approvalservice.controller;

import com.example.agents.approvalservice.dto.ApprovalPatchRequestDto;
import com.example.agents.approvalservice.dto.PipelineStepRequestDto;
import com.example.agents.approvalservice.facade.IPipelineFacade;
import com.example.agents.common.dto.PipelineMessageDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalControllerTest {

    @Mock
    private IPipelineFacade pipelineFacade;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ApprovalController controller;

    @Test
    void requestApproval_returnsFacadeResponse() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        PipelineMessageDto expected = PipelineMessageDto.builder().jobId("job-1").build();
        when(pipelineFacade.process(request)).thenReturn(expected);

        var response = controller.requestApproval(request);

        assertThat(response.getBody()).isSameAs(expected);
        verify(pipelineFacade).process(request);
    }

    @Test
    void patchApproval_postsEventPayloadToConductorEndpoint() {
        ReflectionTestUtils.setField(controller, "conductorEventEndpoint", "http://conductor/api/events");
        ApprovalPatchRequestDto patch = new ApprovalPatchRequestDto();
        patch.setDecision("APPROVED");
        patch.setReviewer("alice");
        patch.setPatchedValues(Map.of("total", 25));

        var response = controller.patchApproval("job-123", patch);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        verify(restTemplate).postForEntity(eq("http://conductor/api/events/approval.job-123"), eq(Map.of(
                "jobId", "job-123",
                "decision", "APPROVED",
                "reviewer", "alice",
                "patchedValues", Map.of("total", 25)
        )), eq(Void.class));
    }
}
