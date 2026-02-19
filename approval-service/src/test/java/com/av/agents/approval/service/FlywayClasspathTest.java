package com.av.agents.approval.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FlywayClasspathTest {

  @Test
  void flywayMigrationsAreOnClasspath() {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    assertThat(classLoader.getResource("db/migration"))
        .as("db/migration on classpath")
        .isNotNull();
    assertThat(classLoader.getResource("db/migration/V2__create_approval_request.sql"))
        .as("approval migration on classpath")
        .isNotNull();
  }

  @Test
  void flywayDependencyIsOnClasspath() throws ClassNotFoundException {
    assertThat(Class.forName("org.flywaydb.core.Flyway")).isNotNull();
  }

}
