package com.av.agents.classificationworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.av.agents.classificationworker.entity")
@EnableJpaRepositories("com.av.agents.classificationworker.repository")
public class ClassificationWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClassificationWorkerApplication.class, args);
    }
}
