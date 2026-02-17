package com.example.agents.financialextractionworker.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration
public class PostgresTestcontainersConfig {

    // Reuse one container per JVM; avoids repeated startup cost across tests in this module.
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("financial_extraction_test")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Ensure Postgres is used (not H2 auto-detected), and keep validation meaningful.
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        // Usually required in tests to ensure schema exists.
        // Pick ONE: Flyway OR Hibernate DDL. Prefer Flyway if you have migrations.
        registry.add("spring.flyway.enabled", () -> "true");
    }
}
