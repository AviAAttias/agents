package com.example.agents.classificationworker.service;

import com.example.agents.classificationworker.dto.PipelineStepRequestDto;
import com.example.agents.classificationworker.entity.PipelineStepEntity;
import com.example.agents.classificationworker.mapper.IPipelineStepMapper;
import com.example.agents.classificationworker.repository.IPipelineStepRepository;
import com.example.agents.common.ai.OpenAiJsonClient;
import com.example.agents.common.ai.OpenAiJsonRequest;
import com.example.agents.common.ai.OpenAiJsonResponse;
import com.example.agents.common.ai.PipelineTaskException;
import com.example.agents.common.enums.PipelineStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineStepServiceTest {

    @Mock
    private IPipelineStepRepository repository;
    @Mock
    private IPipelineStepMapper mapper;
    @Mock
    private OpenAiJsonClient openAiJsonClient;
    @Mock
    private Validator validator;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private PipelineStepService service;

    @Test
    void process_createsEntityWhenIdempotencyKeyMissing() throws Exception {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-1");
        request.setTaskType("task-a");
        request.setPayloadJson("{\"text\":\"invoice #123 total due\"}");

        PipelineStepEntity mapped = new PipelineStepEntity();
        mapped.setJobId("job-1");
        mapped.setTaskType("task-a");
        mapped.setArtifactRef("artifact-1");

        var contentNode = new ObjectMapper().readTree(
                "{\"label\":\"invoice\",\"confidence\":0.98,\"reason\":\"contains invoice terms\"}"
        );

        when(repository.findByIdempotencyKey("job-1:task-a")).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(mapped);
        when(openAiJsonClient.completeJson(any(OpenAiJsonRequest.class)))
                .thenReturn(OpenAiJsonResponse.builder().content(contentNode).build());

        var response = service.process(request);

        assertThat(response.getStatus()).isEqualTo(PipelineStatus.PROCESSED);
        assertThat(response.getPayloadJson()).contains("\"label\":\"invoice\"");
        verify(repository).save(mapped);
    }


    @Test
    void process_reusesExistingEntityWhenIdempotencyKeyFound() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-2");
        request.setTaskType("task-b");
        request.setPayloadJson("updated invoice text");

        PipelineStepEntity existing = new PipelineStepEntity();
        existing.setJobId("job-2");
        existing.setTaskType("task-b");
        existing.setPayloadJson("old");

        when(repository.findByIdempotencyKey("job-2:task-b")).thenReturn(Optional.of(existing));

        var response = service.process(request);

        assertThat(response.getPayloadJson()).isEqualTo("updated invoice text");
        assertThat(existing.getUpdatedAt()).isAfter(OffsetDateTime.now().minusMinutes(1));
        verify(openAiJsonClient, never()).completeJson(any(OpenAiJsonRequest.class));
        verify(repository).save(existing);
    }

    @Test
    void process_rejectsBlankClassificationText() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-3");
        request.setTaskType("task-c");
        request.setPayloadJson("{\"text\":\"\"}");

        PipelineStepEntity mapped = new PipelineStepEntity();
        mapped.setJobId("job-3");
        mapped.setTaskType("task-c");

        when(repository.findByIdempotencyKey("job-3:task-c")).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(mapped);

        PipelineTaskException ex = assertThrows(PipelineTaskException.class, () -> service.process(request));
        assertThat(ex.getErrorCode()).isEqualTo("INVALID_INPUT");
    }
}
