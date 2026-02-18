package com.av.agents.approval.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.av.agents.sharedpersistence.entity.ApprovalRequestEntity;
import com.av.agents.sharedpersistence.entity.ApprovalStatus;
import com.av.agents.sharedpersistence.repository.IApprovalRequestRepository;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.flyway.locations=classpath:db/migration",
    "spring.flyway.validate-on-migrate=true",
    "spring.flyway.fail-on-missing-locations=true",
    "spring.datasource.url=jdbc:h2:mem:sharedschema;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=validate",
    "logging.level.org.flywaydb=DEBUG"
})
@ContextConfiguration(initializers = SharedSchemaValidationIntegrationTest.FlywayContextInitializer.class)
class SharedSchemaValidationIntegrationTest {

  @Autowired
  private IApprovalRequestRepository repository;

  @Test
  void contextStartsAndSharedRepositoryIsUsable() {
    ApprovalRequestEntity entity = new ApprovalRequestEntity();
    entity.setJobId("job-it-1");
    entity.setStatus(ApprovalStatus.PENDING);
    entity.setExpiresAt(Instant.now().plusSeconds(3600));

    ApprovalRequestEntity saved = repository.saveAndFlush(entity);
    assertThat(saved.getId()).isNotNull();
    assertThat(repository.findByJobId("job-it-1")).isPresent();
  }

  static class FlywayContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      String url = applicationContext.getEnvironment().getProperty("spring.datasource.url");
      String username = applicationContext.getEnvironment().getProperty("spring.datasource.username", "sa");
      String password = applicationContext.getEnvironment().getProperty("spring.datasource.password", "");

      Flyway flyway = Flyway.configure()
          .locations("classpath:db/migration")
          .validateOnMigrate(true)
          .failOnMissingLocations(true)
          .cleanDisabled(false)
          .dataSource(url, username, password)
          .load();
      flyway.clean();
      flyway.migrate();
    }
  }
}
