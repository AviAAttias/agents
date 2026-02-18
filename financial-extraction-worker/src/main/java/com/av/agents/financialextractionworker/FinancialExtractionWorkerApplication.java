package com.av.agents.financialextractionworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.av.agents", "com.av.agents.sharedpersistence.entity"})
@EnableJpaRepositories(basePackages = {"com.av.agents", "com.av.agents.sharedpersistence.repository"})
public class FinancialExtractionWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinancialExtractionWorkerApplication.class, args);
    }
}
