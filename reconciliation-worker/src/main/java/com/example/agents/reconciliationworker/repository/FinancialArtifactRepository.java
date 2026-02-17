package com.example.agents.reconciliationworker.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class FinancialArtifactRepository {
    private final JdbcTemplate jdbcTemplate;

    public FinancialArtifactRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> findCanonicalJsonById(long id) {
        return jdbcTemplate.query(
                "SELECT canonical_json FROM financial_artifact WHERE id = ?",
                ps -> ps.setLong(1, id),
                rs -> rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty()
        );
    }
}
