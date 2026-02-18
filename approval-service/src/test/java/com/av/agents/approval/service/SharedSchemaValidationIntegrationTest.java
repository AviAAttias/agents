package com.av.agents.approval.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.av.agents.sharedpersistence.entity.ApprovalRequestEntity;
import com.av.agents.sharedpersistence.entity.ApprovalStatus;
import com.av.agents.sharedpersistence.repository.IApprovalRequestRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration",
    "spring.datasource.url=jdbc:h2:mem:sharedschema;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=validate"
})
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
}
