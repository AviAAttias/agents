package com.example.agents.reportingworker.service;

import com.example.agents.common.enums.PipelineStatus;
import com.example.agents.reportingworker.dto.PipelineStepRequestDto;
import com.example.agents.reportingworker.entity.PipelineStepEntity;
import com.example.agents.reportingworker.mapper.IPipelineStepMapper;
import com.example.agents.reportingworker.repository.IPipelineStepRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineStepServiceTest {

    @Mock
    private IPipelineStepRepository repository;
    @Mock
    private IPipelineStepMapper mapper;
    @InjectMocks
    private PipelineStepService service;

    @Test
    void process_createsEntityWhenIdempotencyKeyMissing() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-1");
        request.setTaskType("task-a");
        request.setPayloadJson("{\"value\":1}");

        PipelineStepEntity mapped = new PipelineStepEntity();
        mapped.setJobId("job-1");
        mapped.setTaskType("task-a");
        mapped.setArtifactRef("artifact-1");

        when(repository.findByIdempotencyKey("job-1:task-a")).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(mapped);

        var response = service.process(request);

        assertThat(response.getStatus()).isEqualTo(PipelineStatus.PROCESSED);
        assertThat(response.getJobId()).isEqualTo("job-1");
        assertThat(response.getTaskType()).isEqualTo("task-a");
        assertThat(response.getArtifactRef()).isEqualTo("artifact-1");
        assertThat(response.getPayloadJson()).isEqualTo("{\"value\":1}");
        assertThat(response.getProcessedAt()).isNotNull();
        assertThat(mapped.getStatus()).isEqualTo(PipelineStatus.PROCESSED.name());
        assertThat(mapped.getUpdatedAt()).isNotNull();
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

        var response = service.process(request);

        assertThat(response.getPayloadJson()).isEqualTo("updated");
        assertThat(existing.getPayloadJson()).isEqualTo("updated");
        assertThat(existing.getUpdatedAt()).isAfter(OffsetDateTime.now().minusMinutes(1));
        verify(mapper, never()).toEntity(any());
        verify(repository).save(existing);
    }
}
