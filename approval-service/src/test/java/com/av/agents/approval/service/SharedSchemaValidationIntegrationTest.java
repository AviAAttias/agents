package com.av.agents.approval.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.av.agents.sharedpersistence.entity.ApprovalRequestEntity;
import com.av.agents.sharedpersistence.entity.ApprovalStatus;
import com.av.agents.sharedpersistence.repository.IApprovalRequestRepository;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration,classpath:db/migration/shared",
    "spring.flyway.validate-on-migrate=true",
    "spring.flyway.fail-on-missing-locations=true",
    "spring.flyway.default-schema=shared",
    "spring.flyway.schemas=shared",
    "spring.flyway.create-schemas=true",
    "spring.jpa.properties.hibernate.default_schema=shared",
    "spring.datasource.url=jdbc:h2:mem:sharedschema;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.defer-datasource-initialization=false",
    "logging.level.org.flywaydb=DEBUG",
    "logging.level.org.springframework.boot.autoconfigure.flyway=DEBUG"
})
class SharedSchemaValidationIntegrationTest {

  @Autowired
  private IApprovalRequestRepository repository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void contextStartsAndSharedRepositoryIsUsable() {
    Integer appliedMigrations = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM shared.flyway_schema_history WHERE success = TRUE", Integer.class);
    assertThat(appliedMigrations).isNotNull();
    assertThat(appliedMigrations).isGreaterThanOrEqualTo(3);

    ApprovalRequestEntity entity = new ApprovalRequestEntity();
    entity.setJobId("job-it-1");
    entity.setStatus(ApprovalStatus.PENDING);
    entity.setExpiresAt(Instant.now().plusSeconds(3600));

    ApprovalRequestEntity saved = repository.saveAndFlush(entity);
    assertThat(saved.getId()).isNotNull();
    assertThat(repository.findByJobId("job-it-1")).isPresent();
  }
}
