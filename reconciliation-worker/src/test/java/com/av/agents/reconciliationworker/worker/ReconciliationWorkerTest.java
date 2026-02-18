package com.av.agents.reconciliationworker.worker;

import com.av.agents.reconciliationworker.dto.ReconciliationResultDto;
import com.av.agents.reconciliationworker.facade.ReconciliationFacade;
import com.netflix.conductor.common.metadata.tasks.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationWorkerTest {

    @Mock
    private ReconciliationFacade reconciliationFacade;

    @InjectMocks
    private ReconciliationWorker worker;

    @Test
    void execute_contractIncludesArtifactRef() {
        Task task = new Task();
        task.setTaskId("t-1");
        task.setWorkflowInstanceId("w-1");
        task.setInputData(Map.of("jobId", "job-1", "financialArtifact", "fin:10"));

        when(reconciliationFacade.reconcile("job-1", "fin:10")).thenReturn(ReconciliationResultDto.builder()
                .artifactRef("val:55")
                .validationStatus("PASSED")
                .violationCount(0)
                .build());

        var result = worker.execute(task);

        assertThat(result.getOutputData()).containsEntry("artifactRef", "val:55");
        assertThat(result.getStatus().name()).isEqualTo("COMPLETED");
    }
}
