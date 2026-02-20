package com.av.agents.textextractionworker.worker;

import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.textextractionworker.dto.TextExtractionResultDto;
import com.av.agents.textextractionworker.service.ITextExtractionPipelineService;
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
class ExtractTextConductorWorkerTest {

    @Mock
    private ITextExtractionPipelineService textExtractionPipelineService;

    @InjectMocks
    private ExtractTextConductorWorker worker;

    private static Task baseTask(String taskId, Map<String, Object> input) {
        Task task = new Task();
        task.setTaskId(taskId);
        task.setWorkflowInstanceId("wf-1");
        task.setStatus(Task.Status.IN_PROGRESS); // Required for TaskResult constructor
        task.setReferenceTaskName("extract_text");
        task.setInputData(input);
        return task;
    }

    @Test
    void execute_returnsConductorContractKeys() {
        Task task = baseTask(
                "task-1",
                Map.of("jobId", "job-1", "artifact", "file:///tmp/invoice.pdf")
        );

        when(textExtractionPipelineService.process(any())).thenReturn(
                TextExtractionResultDto.builder()
                        .text("hello")
                        .artifactRef("text-artifact://17")
                        .textArtifact("text-artifact://17")
                        .inputBytes(22)
                        .outputChars(5)
                        .durationMs(15)
                        .wasTruncated(false)
                        .pageCount(1)
                        .build()
        );

        TaskResult result = worker.execute(task);

        assertThat(worker.getTaskDefName()).isEqualTo("extract_text");
        assertThat(result.getStatus()).isEqualTo(TaskResult.Status.COMPLETED);
        assertThat(result.getOutputData()).containsKeys("text", "artifactRef");
        assertThat(result.getOutputData().get("text")).isEqualTo("hello");
        assertThat(result.getOutputData().get("artifactRef")).isEqualTo("text-artifact://17");
    }

    @Test
    void execute_onExtractionFailure_returnsStableErrorPayload() {
        Task task = baseTask(
                "task-2",
                Map.of("jobId", "job-1", "artifact", "missing.pdf")
        );

        when(textExtractionPipelineService.process(any()))
                .thenThrow(new PipelineTaskException("ARTIFACT_NOT_FOUND", "Artifact missing"));

        TaskResult result = worker.execute(task);

        assertThat(result.getStatus()).isEqualTo(TaskResult.Status.FAILED);
        assertThat(result.getOutputData())
                .containsEntry("errorCode", "ARTIFACT_NOT_FOUND")
                .containsEntry("errorMessage", "Artifact missing");
        assertThat(result.getReasonForIncompletion())
                .isEqualTo("ARTIFACT_NOT_FOUND: Artifact missing");
    }
}
