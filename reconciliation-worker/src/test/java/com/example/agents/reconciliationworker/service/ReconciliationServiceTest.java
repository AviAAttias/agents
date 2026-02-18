package com.example.agents.reconciliationworker.service;

import com.example.agents.reconciliationworker.dto.ReconciliationResultDto;
import com.example.agents.reconciliationworker.entity.ValidationArtifactEntity;
import com.example.agents.reconciliationworker.repository.FinancialArtifactRepository;
import com.example.agents.reconciliationworker.repository.ValidationArtifactRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {
    @Mock
    private FinancialArtifactRepository financialArtifactRepository;
    @Mock
    private ValidationArtifactRepository validationArtifactRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reconciliationService, "toleranceAbs", new java.math.BigDecimal("0.01"));
    }

    @Test
    void reconcile_appliesToleranceLogic() throws Exception {
        when(validationArtifactRepository.findByJobIdAndTaskType("job-1", "validate_reconcile")).thenReturn(Optional.empty());
        when(financialArtifactRepository.findCanonicalJsonById(7L)).thenReturn(Optional.of("""
                {"documentType":"INVOICE","currency":"USD","periodStart":"2024-01-01","periodEnd":"2024-01-31","totalAmount":100.01,
                 "lineItems":[{"amount":40.00,"currency":"USD"},{"amount":60.00,"currency":"USD"}]}
                """));
        when(validationArtifactRepository.save(any())).thenAnswer(invocation -> {
            ValidationArtifactEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            entity.setCreatedAt(OffsetDateTime.now());
            return entity;
        });

        ReconciliationResultDto result = reconciliationService.reconcile("job-1", "fin:7");

        assertThat(result.getArtifactRef()).isEqualTo("val:99");
        ArgumentCaptor<ValidationArtifactEntity> captor = ArgumentCaptor.forClass(ValidationArtifactEntity.class);
        verify(validationArtifactRepository).save(captor.capture());
        JsonNode validationJson = objectMapper.readTree(captor.getValue().getValidationJson());
        assertThat(validationJson.path("validationStatus").asText()).isEqualTo("PASSED");
        assertThat(validationJson.path("violations")).isEmpty();
    }

    @Test
    void reconcile_coversRuleMatrixFailures() throws Exception {
        when(validationArtifactRepository.findByJobIdAndTaskType("job-2", "validate_reconcile")).thenReturn(Optional.empty());
        when(financialArtifactRepository.findCanonicalJsonById(anyLong())).thenReturn(Optional.of("""
                {"documentType":"INVOICE","currency":"USD","periodStart":"2024-02-10","periodEnd":"2024-02-01","totalAmount":-12.00,
                 "lineItems":[{"amount":10.00,"currency":"EUR"}]}
                """));
        when(validationArtifactRepository.save(any())).thenAnswer(invocation -> {
            ValidationArtifactEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return entity;
        });

        reconciliationService.reconcile("job-2", "fin:8");

        ArgumentCaptor<ValidationArtifactEntity> captor = ArgumentCaptor.forClass(ValidationArtifactEntity.class);
        verify(validationArtifactRepository).save(captor.capture());
        JsonNode validationJson = objectMapper.readTree(captor.getValue().getValidationJson());
        assertThat(validationJson.path("validationStatus").asText()).isEqualTo("FAILED");
        assertThat(validationJson.path("violations").toString())
                .contains("TOTAL_MISMATCH", "CURRENCY_MISMATCH", "INVALID_DATE_RANGE", "NEGATIVE_TOTAL_NOT_ALLOWED");
    }

    @Test
    void reconcile_appliesDocTypeSpecificRequiredRules() throws Exception {
        when(validationArtifactRepository.findByJobIdAndTaskType("job-3", "validate_reconcile")).thenReturn(Optional.empty());
        when(financialArtifactRepository.findCanonicalJsonById(9L)).thenReturn(Optional.of("""
                {"documentType":"BANK_STATEMENT","currency":"USD","lineItems":[]}
                """));
        when(validationArtifactRepository.save(any())).thenAnswer(invocation -> {
            ValidationArtifactEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            return entity;
        });

        reconciliationService.reconcile("job-3", "fin:9");

        ArgumentCaptor<ValidationArtifactEntity> captor = ArgumentCaptor.forClass(ValidationArtifactEntity.class);
        verify(validationArtifactRepository).save(captor.capture());
        JsonNode validationJson = objectMapper.readTree(captor.getValue().getValidationJson());
        assertThat(validationJson.path("violations").toString()).contains("$.periodStart", "$.periodEnd");
    }
}
