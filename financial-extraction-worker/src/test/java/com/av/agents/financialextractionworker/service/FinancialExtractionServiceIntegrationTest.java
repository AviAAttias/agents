package com.av.agents.financialextractionworker.service;

import com.av.agents.common.ai.OpenAiJsonClient;
import com.av.agents.common.ai.OpenAiJsonRequest;
import com.av.agents.common.ai.OpenAiJsonResponse;
import com.av.agents.financialextractionworker.dto.FinancialExtractionRequestDto;
import com.av.agents.financialextractionworker.dto.FinancialExtractionResultDto;
import com.av.agents.sharedpersistence.entity.FinancialArtifactEntity;
import com.av.agents.sharedpersistence.repository.IFinancialArtifactRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "conductor.enabled=false",
        "spring.cloud.config.enabled=false",
        "ai.openai.api-key=test-key"
})
@ActiveProfiles("test")
class FinancialExtractionServiceIntegrationTest {

    @Autowired
    private FinancialExtractionService service;

    @Autowired
    private IFinancialArtifactRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OpenAiJsonClient openAiJsonClient;

    @Test
    void createsDbRowAndIsIdempotent() throws Exception {
        when(openAiJsonClient.completeJson(any(OpenAiJsonRequest.class))).thenReturn(OpenAiJsonResponse.builder()
                .content(objectMapper.readTree(validResponseJson()))
                .outputChars(200)
                .build());

        FinancialExtractionRequestDto request = FinancialExtractionRequestDto.builder()
                .jobId("job-int-1")
                .docType("INVOICE")
                .text("Invoice total is 1234.56 USD")
                .taskType("extract_financials")
                .build();

        FinancialExtractionResultDto first = service.extract(request);
        FinancialExtractionResultDto second = service.extract(request);

        List<FinancialArtifactEntity> rows = repository.findAll();
        assertThat(rows).hasSize(1);
        assertThat(first.getArtifactRef()).isEqualTo(second.getArtifactRef());
        verify(openAiJsonClient, times(1)).completeJson(any(OpenAiJsonRequest.class));
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
