package com.av.agents.reconciliationworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.av.agents", "com.av.agents.sharedpersistence.entity"})
@EnableJpaRepositories(basePackages = {"com.av.agents", "com.av.agents.sharedpersistence.repository"})
public class ReconciliationWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReconciliationWorkerApplication.class, args);
    }
}
