package com.av.agents.pdfingestionservice.worker;

import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.pdfingestionservice.dto.PdfIngestionResultDto;
import com.av.agents.pdfingestionservice.service.PdfIngestionPipelineService;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestPdfConductorWorkerTest {

    @Mock
    private PdfIngestionPipelineService pipelineService;

    @InjectMocks
    private IngestPdfConductorWorker worker;

    @Test
    void execute_contractContainsArtifactRef() {
        Task task = baseTask("task-1", Map.of("jobId", "job-1", "pdfUrl", "http://example.com/a.pdf"));

        when(pipelineService.process(any())).thenReturn(PdfIngestionResultDto.builder()
                .artifactRef("pdf:abc")
                .sha256("abc")
                .bytes(123)
                .durationMs(20)
                .build());

        TaskResult result = worker.execute(task);

        assertThat(result.getStatus()).isEqualTo(TaskResult.Status.COMPLETED);
        assertThat(result.getOutputData()).containsKey("artifactRef");
        assertThat(result.getOutputData().get("artifactRef")).isEqualTo("pdf:abc");
    }

    @Test
    void execute_failureMapsErrorContract() {
        Task task = baseTask("task-2", Map.of("jobId", "job-2", "pdfUrl", "bad"));
        when(pipelineService.process(any())).thenThrow(new PipelineTaskException("PDF_FETCH_FAILED", "boom"));

        TaskResult result = worker.execute(task);

        assertThat(result.getStatus()).isEqualTo(TaskResult.Status.FAILED);
        assertThat(result.getOutputData()).containsEntry("errorCode", "PDF_FETCH_FAILED");
    }

    private static Task baseTask(String taskId, Map<String, Object> input) {
        Task task = new Task();
        task.setTaskId(taskId);
        task.setWorkflowInstanceId("wf-1");
        task.setReferenceTaskName("ingest_pdf");
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(input);
        return task;
    }
}
