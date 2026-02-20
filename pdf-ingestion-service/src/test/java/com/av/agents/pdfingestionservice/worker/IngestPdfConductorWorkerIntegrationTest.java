package com.av.agents.pdfingestionservice.worker;

import com.av.agents.pdfingestionservice.entity.PdfArtifactEntity;
import com.av.agents.pdfingestionservice.entity.PipelineStepEntity;
import com.av.agents.pdfingestionservice.repository.IPdfArtifactRepository;
import com.av.agents.pdfingestionservice.repository.IPipelineStepRepository;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "conductor.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class IngestPdfConductorWorkerIntegrationTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:15.10");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    private static HttpServer server;
    private static final AtomicInteger fetchCounter = new AtomicInteger(0);
    private static Path artifactsDir;

    @BeforeAll
    static void startHttpServer() throws IOException {
        artifactsDir = Files.createTempDirectory("pdf-artifacts-it");
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/invoice.pdf", IngestPdfConductorWorkerIntegrationTest::writePdfResponse);
        server.start();
    }

    @AfterAll
    static void stopHttpServer() throws IOException {
        if (server != null) {
            server.stop(0);
        }
        if (artifactsDir != null) {
            Files.walk(artifactsDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("pdf.ingestion.artifacts-dir", () -> artifactsDir.toString());
        registry.add("pdf.ingestion.max-bytes", () -> "26214400");
        registry.add("pdf.ingestion.timeout-ms", () -> "5000");
    }

    @Autowired
    private IngestPdfConductorWorker worker;

    @Autowired
    private IPipelineStepRepository pipelineStepRepository;

    @Autowired
    private IPdfArtifactRepository pdfArtifactRepository;

    @Test
    void execute_firstRunPersistsAndSecondRunUsesCachedStep() {
        String pdfUrl = "http://localhost:" + server.getAddress().getPort() + "/invoice.pdf";

        Task firstTask = baseTask("task-1", "job-123", pdfUrl);
        TaskResult first = worker.execute(firstTask);

        assertThat(first.getStatus()).isEqualTo(TaskResult.Status.COMPLETED);
        assertThat(first.getOutputData()).containsKeys("artifactRef", "sha256", "bytes", "durationMs");

        long stepCount = pipelineStepRepository.count();
        long artifactCount = pdfArtifactRepository.count();
        int fetchCount = fetchCounter.get();

        Task secondTask = baseTask("task-2", "job-123", pdfUrl);
        TaskResult second = worker.execute(secondTask);

        assertThat(second.getStatus()).isEqualTo(TaskResult.Status.COMPLETED);
        assertThat(second.getOutputData().get("artifactRef")).isEqualTo(first.getOutputData().get("artifactRef"));
        assertThat(pipelineStepRepository.count()).isEqualTo(stepCount);
        assertThat(pdfArtifactRepository.count()).isEqualTo(artifactCount);
        assertThat(fetchCounter.get()).isEqualTo(fetchCount);

        PipelineStepEntity step = pipelineStepRepository.findByIdempotencyKey("job-123:ingest_pdf").orElseThrow();
        assertThat(step.getStatus()).isEqualTo("PROCESSED");

        PdfArtifactEntity artifact = pdfArtifactRepository.findAll().get(0);
        assertThat(artifact.getJobId()).isEqualTo("job-123");
        assertThat(artifact.getSourceUrl()).isEqualTo(pdfUrl);
    }

    private static Task baseTask(String taskId, String jobId, String pdfUrl) {
        Task task = new Task();
        task.setTaskId(taskId);
        task.setWorkflowInstanceId("wf-1");
        task.setReferenceTaskName("ingest_pdf");
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("jobId", jobId, "pdfUrl", pdfUrl));
        return task;
    }

    private static void writePdfResponse(HttpExchange exchange) throws IOException {
        fetchCounter.incrementAndGet();
        byte[] body = "%PDF-1.4\ncache-test\n%%EOF".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/pdf");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
