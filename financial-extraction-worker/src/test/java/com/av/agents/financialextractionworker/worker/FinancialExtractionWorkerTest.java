package com.av.agents.financialextractionworker.worker;

import com.av.agents.financialextractionworker.dto.FinancialExtractionResultDto;
import com.av.agents.financialextractionworker.facade.FinancialExtractionFacade;
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
class FinancialExtractionWorkerTest {

    @Mock
    private FinancialExtractionFacade facade;

    @InjectMocks
    private FinancialExtractionWorker worker;

    @Test
    void contractOutputContainsExactlyArtifactRef() {
        when(facade.extract(any())).thenReturn(FinancialExtractionResultDto.builder().artifactRef("fin:10").build());

        Task task = new Task();
        task.setTaskId("task-1");
        task.setWorkflowInstanceId("wf-1");
        task.setInputData(Map.of("jobId", "job-1", "docType", "INVOICE", "text", "hello"));

        TaskResult result = worker.execute(task);

        assertThat(result.getStatus()).isEqualTo(TaskResult.Status.COMPLETED);
        assertThat(result.getOutputData()).containsOnlyKeys("artifactRef");
        assertThat(result.getOutputData().get("artifactRef")).isEqualTo("fin:10");
    }
}
