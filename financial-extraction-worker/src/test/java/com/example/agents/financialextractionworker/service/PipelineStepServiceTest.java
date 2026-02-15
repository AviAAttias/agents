package com.example.agents.financialextractionworker.service;

import com.example.agents.common.ai.OpenAiJsonClient;
import com.example.agents.common.ai.OpenAiJsonClientException;
import com.example.agents.common.ai.OpenAiJsonRequest;
import com.example.agents.common.ai.OpenAiJsonResponse;
import com.example.agents.common.ai.OpenAiErrorCode;
import com.example.agents.common.ai.PipelineTaskException;
import com.example.agents.common.enums.PipelineStatus;
import com.example.agents.financialextractionworker.dto.PipelineStepRequestDto;
import com.example.agents.financialextractionworker.entity.PipelineStepEntity;
import com.example.agents.financialextractionworker.mapper.IPipelineStepMapper;
import com.example.agents.financialextractionworker.repository.IPipelineStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
        request.setPayloadJson("Invoice total 120.00 USD");

        PipelineStepEntity mapped = new PipelineStepEntity();
        mapped.setJobId("job-1");
        mapped.setTaskType("task-a");
        mapped.setArtifactRef("artifact-1");

        when(repository.findByIdempotencyKey("job-1:task-a")).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(mapped);

        // IMPORTANT: compute outside the stubbing, using a real ObjectMapper
        var contentNode = new ObjectMapper().readTree(
            "{\"documentType\":\"invoice\",\"invoiceNumber\":\"INV-1\",\"currency\":\"USD\",\"totalAmount\":120.0," +
            "\"taxAmount\":10.0,\"dueDate\":\"2025-10-01\",\"confidence\":0.95,\"explanation\":\"explicit amount\"}"
        );

        when(openAiJsonClient.completeJson(any(OpenAiJsonRequest.class)))
            .thenReturn(OpenAiJsonResponse.builder().content(contentNode).build());

        var response = service.process(request);

        assertThat(response.getStatus()).isEqualTo(PipelineStatus.PROCESSED);
        assertThat(response.getPayloadJson()).contains("\"documentType\":\"invoice\"");
    }


    @Test
    void process_propagatesInvalidSchemaErrorCode() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-2");
        request.setTaskType("task-b");
        request.setPayloadJson("Invoice total 120.00 USD");

        PipelineStepEntity mapped = new PipelineStepEntity();
        mapped.setJobId("job-2");
        mapped.setTaskType("task-b");

        when(repository.findByIdempotencyKey("job-2:task-b")).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(mapped);
        when(openAiJsonClient.completeJson(any(OpenAiJsonRequest.class))).thenThrow(new OpenAiJsonClientException(OpenAiErrorCode.INVALID_SCHEMA_OUTPUT, "bad"));

        PipelineTaskException ex = assertThrows(PipelineTaskException.class, () -> service.process(request));
        assertThat(ex.getErrorCode()).isEqualTo("INVALID_SCHEMA_OUTPUT");
    }
}
