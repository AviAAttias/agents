package com.av.agents.classificationworker.worker;

import com.av.agents.classificationworker.dto.PipelineStepRequestDto;
import com.av.agents.classificationworker.service.IPipelineStepService;
import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.common.enums.PipelineStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClassifyDocConductorWorkerTest {

    @Test
    void outputContractUsesExactDocumentTypeKey() {
        IPipelineStepService service = mock(IPipelineStepService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ClassifyDocConductorWorker worker = new ClassifyDocConductorWorker(service, objectMapper);

        when(service.process(any(PipelineStepRequestDto.class))).thenReturn(PipelineMessageDto.builder()
                .jobId("job-1")
                .taskType("classify_doc")
                .status(PipelineStatus.PROCESSED)
                .payloadJson("{\"documentType\":\"invoice\",\"model\":\"gpt-4o-mini\"}")
                .processedAt(OffsetDateTime.now())
                .build());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS); // <-- critical: prevents TaskResult ctor NPE
        task.setInputData(Map.of("jobId", "job-1", "textArtifact", "text-artifact://1"));

        TaskResult result = worker.execute(task);

        assertThat(result.getStatus()).isEqualTo(TaskResult.Status.COMPLETED);
        assertThat(result.getOutputData()).containsEntry("documentType", "invoice");
        assertThat(result.getOutputData()).doesNotContainKey("label");
    }
}
