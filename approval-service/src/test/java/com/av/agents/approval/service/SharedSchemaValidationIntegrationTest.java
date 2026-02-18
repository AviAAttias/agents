package com.av.agents.approval.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.av.agents.sharedpersistence.entity.ApprovalRequestEntity;
import com.av.agents.sharedpersistence.entity.ApprovalStatus;
import com.av.agents.sharedpersistence.repository.IApprovalRequestRepository;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration",
    "spring.flyway.validate-on-migrate=true",
    "spring.flyway.fail-on-missing-locations=true",
    "spring.datasource.url=jdbc:h2:mem:sharedschema;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=validate",
    "logging.level.org.flywaydb=DEBUG"
})
@Import(SharedSchemaValidationIntegrationTest.JpaDependsOnFlywayConfig.class)
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

  @TestConfiguration
  static class JpaDependsOnFlywayConfig {
    @Bean
    BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
      return new BeanFactoryPostProcessor() {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
          if (beanFactory instanceof BeanDefinitionRegistry registry && registry.containsBeanDefinition("entityManagerFactory")) {
            var beanDefinition = registry.getBeanDefinition("entityManagerFactory");
            String[] dependsOn = beanDefinition.getDependsOn();
            if (dependsOn == null) {
              beanDefinition.setDependsOn("flyway");
              return;
            }
            if (Arrays.stream(dependsOn).noneMatch("flyway"::equals)) {
              String[] merged = Arrays.copyOf(dependsOn, dependsOn.length + 1);
              merged[dependsOn.length] = "flyway";
              beanDefinition.setDependsOn(merged);
            }
          }
        }
      };
    }
  }
}
