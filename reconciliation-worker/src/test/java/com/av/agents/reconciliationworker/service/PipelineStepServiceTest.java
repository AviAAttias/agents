package com.av.agents.reconciliationworker.service;

import com.av.agents.common.enums.PipelineStatus;
import com.av.agents.reconciliationworker.dto.PipelineStepRequestDto;
import com.av.agents.reconciliationworker.entity.PipelineStepEntity;
import com.av.agents.reconciliationworker.mapper.IPipelineStepMapper;
import com.av.agents.reconciliationworker.repository.IPipelineStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineStepServiceTest {

    @Mock
    private IPipelineStepRepository repository;
    @Mock
    private IPipelineStepMapper mapper;
    @Mock
    private OpenAiJsonClient openAiJsonClient;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private PipelineStepService service;

    @Test
    void process_createsEntityWhenIdempotencyKeyMissing() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-1");
        request.setTaskType("task-a");
        request.setPayloadJson("{\"variance\":55}");

        PipelineStepEntity mapped = new PipelineStepEntity();
        mapped.setJobId("job-1");
        mapped.setTaskType("task-a");
        mapped.setArtifactRef("artifact-1");

        when(repository.findByIdempotencyKey("job-1:task-a")).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(mapped);
        when(openAiJsonClient.completeJson(anyString(), anyString(), any())).thenReturn(Optional.of("{\"isAnomaly\":true,\"confidence\":0.88,\"reasoning\":\"variance too high\",\"recommendedAction\":\"manual_review\"}"));

        var response = service.process(request);

        assertThat(response.getStatus()).isEqualTo(PipelineStatus.PROCESSED);
        assertThat(response.getPayloadJson()).contains("manual_review");
        verify(repository).save(mapped);
    }

    @Test
    void process_reusesExistingEntityWhenIdempotencyKeyFound() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-2");
        request.setTaskType("task-b");
        request.setPayloadJson("updated");

        PipelineStepEntity existing = new PipelineStepEntity();
        existing.setJobId("job-2");
        existing.setTaskType("task-b");
        existing.setPayloadJson("old");

        when(repository.findByIdempotencyKey("job-2:task-b")).thenReturn(Optional.of(existing));
        when(openAiJsonClient.completeJson(anyString(), anyString(), any())).thenReturn(Optional.empty());

        var response = service.process(request);

        assertThat(response.getPayloadJson()).contains("run_rule_based_reconciliation");
        assertThat(existing.getUpdatedAt()).isAfter(OffsetDateTime.now().minusMinutes(1));
        verify(mapper, never()).toEntity(any());
        verify(repository).save(existing);
    }
}
