package com.av.agents.financialextractionworker.service;

import com.av.agents.common.ai.OpenAiErrorCode;
import com.av.agents.common.ai.OpenAiJsonClient;
import com.av.agents.common.ai.OpenAiJsonClientException;
import com.av.agents.common.ai.OpenAiJsonRequest;
import com.av.agents.common.ai.OpenAiJsonResponse;
import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.financialextractionworker.dto.FinancialExtractionRequestDto;
import com.av.agents.financialextractionworker.entity.FinancialArtifactEntity;
import com.av.agents.financialextractionworker.repository.FinancialArtifactRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialExtractionServiceTest {

    @Mock
    private OpenAiJsonClient openAiJsonClient;
    @Mock
    private FinancialArtifactRepository repository;
    @InjectMocks
    private FinancialExtractionService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "financialSchemaResource", new ClassPathResource("schema/financial-extraction-v1.json"));
        ReflectionTestUtils.setField(service, "maxTextChars", 10);
        ReflectionTestUtils.setField(service, "configuredModel", "gpt-4o");
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
    }

    @Test
    void truncationBoundaryBehavior_truncatesDeterministically() throws Exception {
        when(repository.findByJobIdAndTaskType("job-1", "extract_financials")).thenReturn(Optional.empty());
        when(openAiJsonClient.completeJson(any(OpenAiJsonRequest.class))).thenReturn(OpenAiJsonResponse.builder()
                .content(objectMapper.readTree(validResponseJson()))
                .outputChars(120)
                .build());
        when(repository.save(any(FinancialArtifactEntity.class))).thenAnswer(invocation -> {
            FinancialArtifactEntity entity = invocation.getArgument(0);
            entity.setId(7L);
            return entity;
        });

        service.extract(baseRequest("0123456789ABCDEFGHIJ"));

        ArgumentCaptor<FinancialArtifactEntity> entityCaptor = ArgumentCaptor.forClass(FinancialArtifactEntity.class);
        verify(repository).save(entityCaptor.capture());
        FinancialArtifactEntity saved = entityCaptor.getValue();
        assertThat(saved.isWasTruncated()).isTrue();
        assertThat(saved.getInputCharCount()).isEqualTo(20);

        ArgumentCaptor<OpenAiJsonRequest> aiRequestCaptor = ArgumentCaptor.forClass(OpenAiJsonRequest.class);
        verify(openAiJsonClient).completeJson(aiRequestCaptor.capture());
        assertThat(aiRequestCaptor.getValue().getUserPrompt()).contains("0123456789");
        assertThat(aiRequestCaptor.getValue().getUserPrompt()).doesNotContain("A");
    }

    @Test
    void schemaValidationFailure_rejectsInvalidSchemaOutput() throws Exception {
        when(repository.findByJobIdAndTaskType("job-1", "extract_financials")).thenReturn(Optional.empty());
        when(openAiJsonClient.completeJson(any(OpenAiJsonRequest.class))).thenReturn(OpenAiJsonResponse.builder()
                .content(objectMapper.readTree("{\"documentType\":\"INVOICE\"}"))
                .build());

        PipelineTaskException ex = assertThrows(PipelineTaskException.class, () -> service.extract(baseRequest("short text")));
        assertThat(ex.getErrorCode()).isEqualTo("INVALID_SCHEMA_OUTPUT");
    }

    @Test
    void docTypeGatingLogic_rejectsUnsupportedDocType() {
        when(repository.findByJobIdAndTaskType("job-1", "extract_financials")).thenReturn(Optional.empty());

        FinancialExtractionRequestDto request = FinancialExtractionRequestDto.builder()
                .jobId("job-1")
                .docType("MEMO")
                .text("some text")
                .taskType("extract_financials")
                .build();

        PipelineTaskException ex = assertThrows(PipelineTaskException.class, () -> service.extract(request));
        assertThat(ex.getErrorCode()).isEqualTo("INVALID_INPUT");
        verifyNoInteractions(openAiJsonClient);
    }

    @Test
    void retriesOnceForTransientErrorsOnly() throws Exception {
        when(repository.findByJobIdAndTaskType("job-1", "extract_financials")).thenReturn(Optional.empty());
        when(openAiJsonClient.completeJson(any(OpenAiJsonRequest.class)))
                .thenThrow(new OpenAiJsonClientException(OpenAiErrorCode.TIMEOUT, "timeout"))
                .thenReturn(OpenAiJsonResponse.builder().content(objectMapper.readTree(validResponseJson())).build());
        when(repository.save(any(FinancialArtifactEntity.class))).thenAnswer(invocation -> {
            FinancialArtifactEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            return entity;
        });

        service.extract(baseRequest("short text"));

        verify(openAiJsonClient, times(2)).completeJson(any(OpenAiJsonRequest.class));
    }

    private FinancialExtractionRequestDto baseRequest(String text) {
        return FinancialExtractionRequestDto.builder()
                .jobId("job-1")
                .docType("invoice")
                .text(text)
                .taskType("extract_financials")
                .build();
    }

    private String validResponseJson() {
        return """
                {
                  "documentType":"INVOICE",
                  "currency":"USD",
                  "periodStart":"2025-01-01",
                  "periodEnd":"2025-01-31",
                  "totalAmount":1234.56,
                  "lineItems":[{"description":"Service Fee","amount":1000.0,"currency":"USD","page":2}],
                  "confidence":0.92,
                  "provenance":{"model":"gpt-4o","schemaVersion":"v1"}
                }
                """;
    }
}
