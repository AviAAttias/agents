package com.example.agents.classificationworker.service;

import com.example.agents.classificationworker.dto.PipelineStepRequestDto;
import com.example.agents.classificationworker.entity.PipelineStepEntity;
import com.example.agents.classificationworker.entity.TextArtifactEntity;
import com.example.agents.classificationworker.mapper.IPipelineStepMapper;
import com.example.agents.classificationworker.repository.IClassificationArtifactRepository;
import com.example.agents.classificationworker.repository.IPipelineStepRepository;
import com.example.agents.classificationworker.repository.ITextArtifactRepository;
import com.example.agents.common.ai.OpenAiJsonClient;
import com.example.agents.common.ai.OpenAiJsonRequest;
import com.example.agents.common.ai.OpenAiJsonResponse;
import com.example.agents.common.ai.PipelineTaskException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineStepServiceTest {

    @Mock
    private IPipelineStepRepository pipelineStepRepository;
    @Mock
    private IPipelineStepMapper pipelineStepMapper;
    @Mock
    private ITextArtifactRepository textArtifactRepository;
    @Mock
    private IClassificationArtifactRepository classificationArtifactRepository;
    @Mock
    private OpenAiJsonClient openAiJsonClient;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private PipelineStepService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxTextChars", 12000);
        ReflectionTestUtils.setField(service, "openAiModel", "gpt-4o-mini");
    }

    @Test
    void mapsSchemaValidJsonToDocumentType() throws Exception {
        PipelineStepRequestDto request = request("job-1", "classify_doc", "text-artifact://42");
        PipelineStepEntity mapped = mappedEntity("job-1", "classify_doc");
        TextArtifactEntity textArtifact = new TextArtifactEntity();
        textArtifact.setId(42L);
        textArtifact.setTextBody("invoice text body");

        // Build JsonNode BEFORE stubbing (do not do work inside when(...).thenReturn(...))
        var contentNode = objectMapper.readTree("{\"documentType\":\"invoice\"}");

        when(pipelineStepRepository.findByIdempotencyKey("job-1:classify_doc")).thenReturn(Optional.empty());
        when(pipelineStepMapper.toEntity(request)).thenReturn(mapped);
        when(textArtifactRepository.findById(42L)).thenReturn(Optional.of(textArtifact));

        when(openAiJsonClient.completeJson(any(OpenAiJsonRequest.class)))
                .thenReturn(OpenAiJsonResponse.builder()
                        .content(contentNode)
                        .outputChars(26)
                        .build());

        var response = service.process(request);

        assertThat(response.getPayloadJson()).contains("\"documentType\":\"invoice\"");
        verify(classificationArtifactRepository).save(any());
    }

    @Test
    void throwsTypedErrorOnSchemaInvalidMapping() throws Exception {
        PipelineStepRequestDto request = request("job-2", "classify_doc", "text-artifact://7");
        PipelineStepEntity mapped = mappedEntity("job-2", "classify_doc");
        TextArtifactEntity textArtifact = new TextArtifactEntity();
        textArtifact.setId(7L);
        textArtifact.setTextBody("bank statement text");

        when(pipelineStepRepository.findByIdempotencyKey("job-2:classify_doc")).thenReturn(Optional.empty());
        when(pipelineStepMapper.toEntity(request)).thenReturn(mapped);
        when(textArtifactRepository.findById(7L)).thenReturn(Optional.of(textArtifact));

        var invalidContent = objectMapper.readTree("{}"); // compute outside Mockito chain
        when(openAiJsonClient.completeJson(any(OpenAiJsonRequest.class)))
                .thenReturn(OpenAiJsonResponse.builder()
                        .content(invalidContent)
                        .outputChars(2)
                        .build());

        assertThatThrownBy(() -> service.process(request))
                .isInstanceOf(PipelineTaskException.class)
                .extracting(ex -> ((PipelineTaskException) ex).getErrorCode())
                .isEqualTo("INVALID_SCHEMA_OUTPUT");
    }

    @Test
    void usesIdempotentCacheWithoutCallingOpenAi() {
        PipelineStepRequestDto request = request("job-3", "classify_doc", "text-artifact://9");

        PipelineStepEntity existing = mappedEntity("job-3", "classify_doc");
        existing.setPayloadJson("{\"documentType\":\"receipt\"}");

        when(pipelineStepRepository.findByIdempotencyKey("job-3:classify_doc")).thenReturn(Optional.of(existing));

        var response = service.process(request);

        assertThat(response.getPayloadJson()).isEqualTo("{\"documentType\":\"receipt\"}");
        verify(openAiJsonClient, never()).completeJson(any(OpenAiJsonRequest.class));
        verify(classificationArtifactRepository, never()).save(any());
    }

    @Test
    void truncatesUsingConfiguredLimitBeforeModelCall() {
        ReflectionTestUtils.setField(service, "maxTextChars", 10);

        PipelineStepRequestDto request = request("job-4", "classify_doc", "text-artifact://10");
        PipelineStepEntity mapped = mappedEntity("job-4", "classify_doc");
        TextArtifactEntity textArtifact = new TextArtifactEntity();
        textArtifact.setId(10L);
        textArtifact.setTextBody("12345678901234567890");

        when(pipelineStepRepository.findByIdempotencyKey("job-4:classify_doc")).thenReturn(Optional.empty());
        when(pipelineStepMapper.toEntity(request)).thenReturn(mapped);
        when(textArtifactRepository.findById(10L)).thenReturn(Optional.of(textArtifact));
        when(openAiJsonClient.completeJson(any(OpenAiJsonRequest.class)))
                .thenReturn(OpenAiJsonResponse.builder().content(new ObjectMapper().createObjectNode().put("documentType", "other")).build());

        service.process(request);

        ArgumentCaptor<OpenAiJsonRequest> captor = ArgumentCaptor.forClass(OpenAiJsonRequest.class);
        verify(openAiJsonClient).completeJson(captor.capture());
        assertThat(captor.getValue().getUserPrompt()).contains("1234567890");
    }

    private PipelineStepRequestDto request(String jobId, String taskType, String textArtifactRef) {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId(jobId);
        request.setTaskType(taskType);
        request.setPayloadJson("{\"textArtifact\":\"" + textArtifactRef + "\"}");
        return request;
    }

    private PipelineStepEntity mappedEntity(String jobId, String taskType) {
        PipelineStepEntity mapped = new PipelineStepEntity();
        mapped.setJobId(jobId);
        mapped.setTaskType(taskType);
        mapped.setArtifactRef("artifact://" + jobId + "/" + taskType);
        return mapped;
    }
}
