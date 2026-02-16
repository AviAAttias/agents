package com.example.agents.textextractionworker.config;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ConductorWorkerConfig {

    @Bean
    @ConditionalOnProperty(name = "conductor.enabled", havingValue = "true", matchIfMissing = true)
    public TaskClient taskClient(@Value("${conductor.server.url:http://localhost:8080/api/}") String conductorUrl) {
        TaskClient client = new TaskClient();
        client.setRootURI(conductorUrl);
        return client;
    }

    @Bean(initMethod = "init")
    @ConditionalOnProperty(name = "conductor.enabled", havingValue = "true", matchIfMissing = true)
    public TaskRunnerConfigurer taskRunnerConfigurer(TaskClient taskClient,
                                                     List<Worker> workers,
                                                     @Value("${conductor.worker.thread-count:2}") int threadCount) {
        return new TaskRunnerConfigurer.Builder(taskClient, workers)
                .withThreadCount(threadCount)
                .build();
    }
}
