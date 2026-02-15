package com.example.agents.financialextractionworker.service;

import com.example.agents.common.enums.PipelineStatus;
import com.example.agents.financialextractionworker.dto.PipelineStepRequestDto;
import com.example.agents.financialextractionworker.entity.PipelineStepEntity;
import com.example.agents.financialextractionworker.mapper.IPipelineStepMapper;
import com.example.agents.financialextractionworker.repository.IPipelineStepRepository;
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
        request.setPayloadJson("Invoice total 120.00 USD");

        PipelineStepEntity mapped = new PipelineStepEntity();
        mapped.setJobId("job-1");
        mapped.setTaskType("task-a");
        mapped.setArtifactRef("artifact-1");

        when(repository.findByIdempotencyKey("job-1:task-a")).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(mapped);
        when(openAiJsonClient.completeJson(anyString(), anyString(), any())).thenReturn(Optional.of("{\"documentType\":\"invoice\",\"invoiceNumber\":\"INV-1\",\"currency\":\"USD\",\"totalAmount\":120.0,\"taxAmount\":10.0,\"dueDate\":\"2025-10-01\",\"confidence\":0.95,\"explanation\":\"explicit amount\"}"));

        var response = service.process(request);

        assertThat(response.getStatus()).isEqualTo(PipelineStatus.PROCESSED);
        assertThat(response.getPayloadJson()).contains("\"documentType\":\"invoice\"");
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

        assertThat(response.getPayloadJson()).contains("Deterministic fallback parser");
        assertThat(existing.getUpdatedAt()).isAfter(OffsetDateTime.now().minusMinutes(1));
        verify(mapper, never()).toEntity(any());
        verify(repository).save(existing);
    }
}
