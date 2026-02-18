package com.example.agents.reconciliationworker.service;

import com.example.agents.reconciliationworker.dto.ReconciliationResultDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReconciliationServiceIntegrationTest {

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reconcile_persistsAndSupportsIdempotentRetry() {
        jdbcTemplate.update("INSERT INTO financial_artifact(id, job_id, task_type, canonical_json) VALUES (?,?,?,?)",
                501L, "job-int", "extract_financials",
                "{\"documentType\":\"RECEIPT\",\"currency\":\"USD\",\"totalAmount\":12.00,\"lineItems\":[{\"amount\":12.00,\"currency\":\"USD\"}]}");

        ReconciliationResultDto first = reconciliationService.reconcile("job-int", "fin:501");
        ReconciliationResultDto second = reconciliationService.reconcile("job-int", "fin:501");

        assertThat(first.getArtifactRef()).isEqualTo(second.getArtifactRef());
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM validation_artifact WHERE job_id='job-int' AND task_type='validate_reconcile'", Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
