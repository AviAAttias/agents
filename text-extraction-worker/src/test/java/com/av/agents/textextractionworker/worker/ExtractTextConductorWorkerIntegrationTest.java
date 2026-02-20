package com.av.agents.textextractionworker.worker;

import com.av.agents.textextractionworker.entity.PipelineStepEntity;
import com.av.agents.sharedpersistence.entity.TextArtifactEntity;
import com.av.agents.textextractionworker.repository.IPipelineStepRepository;
import com.av.agents.sharedpersistence.repository.ITextArtifactRepository;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "conductor.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class ExtractTextConductorWorkerIntegrationTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16.3");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ExtractTextConductorWorker worker;

    @Autowired
    private IPipelineStepRepository pipelineStepRepository;

    @Autowired
    private ITextArtifactRepository textArtifactRepository;

    @Test
    void execute_isIdempotentAndReturnsContractKeys(@TempDir Path tempDir) throws Exception {
        Path pdfPath = tempDir.resolve("invoice.pdf");
        Files.copy(getClass().getResourceAsStream("/pdfs/invoice-sample.pdf"), pdfPath);

        Task firstTask = new Task();
        firstTask.setTaskId("task-1");
        firstTask.setWorkflowInstanceId("wf-1");
        firstTask.setInputData(Map.of("jobId", "job-123", "artifact", pdfPath.toString()));

        TaskResult first = worker.execute(firstTask);
        assertThat(first.getStatus()).isEqualTo(TaskResult.Status.COMPLETED);
        assertThat(first.getOutputData()).containsKeys(
                "text", "artifactRef", "textArtifact", "inputBytes", "outputChars",
                "durationMs", "wasTruncated", "pageCount"
        );

        long stepCountAfterFirst = pipelineStepRepository.count();
        long artifactCountAfterFirst = textArtifactRepository.count();

        Task secondTask = new Task();
        secondTask.setTaskId("task-2");
        secondTask.setWorkflowInstanceId("wf-1");
        secondTask.setInputData(Map.of("jobId", "job-123", "artifact", pdfPath.toString()));

        TaskResult second = worker.execute(secondTask);
        assertThat(second.getStatus()).isEqualTo(TaskResult.Status.COMPLETED);
        assertThat(second.getOutputData().get("artifactRef")).isEqualTo(first.getOutputData().get("artifactRef"));
        assertThat(second.getOutputData().get("text")).isEqualTo(first.getOutputData().get("text"));

        assertThat(pipelineStepRepository.count()).isEqualTo(stepCountAfterFirst);
        assertThat(textArtifactRepository.count()).isEqualTo(artifactCountAfterFirst);

        PipelineStepEntity step = pipelineStepRepository.findByIdempotencyKey("job-123:extract_text").orElseThrow();
        assertThat(step.getStatus()).isEqualTo("PROCESSED");

        TextArtifactEntity storedArtifact = textArtifactRepository.findAll().get(0);
        assertThat(storedArtifact.getJobId()).isEqualTo("job-123");
    }
}
