package com.example.agents.reconciliationworker.worker;

import com.example.agents.common.ai.PipelineTaskException;
import com.example.agents.reconciliationworker.dto.ReconciliationResultDto;
import com.example.agents.reconciliationworker.facade.ReconciliationFacade;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationWorker implements Worker {
    private static final String TASK_NAME = "validate_reconcile";

    private final ReconciliationFacade reconciliationFacade;

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult();
        result.setTaskId(task.getTaskId());
        result.setWorkflowInstanceId(task.getWorkflowInstanceId());

        try {
            Map<String, Object> input = task.getInputData() == null ? Map.of() : task.getInputData();
            String jobId = requireString(input, "jobId");
            String financialArtifact = requireString(input, "financialArtifact");

            ReconciliationResultDto response = reconciliationFacade.reconcile(jobId, financialArtifact);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("artifactRef", response.getArtifactRef());
            if (response.getValidationStatus() != null) {
                output.put("validationStatus", response.getValidationStatus());
            }
            output.put("violationCount", response.getViolationCount());

            result.setOutputData(output);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (PipelineTaskException ex) {
            log.warn("event=validate_reconcile_failed taskId={} workflowId={} errorCode={} message={}",
                    task.getTaskId(), task.getWorkflowInstanceId(), ex.getErrorCode(), ex.getMessage());
            result.setOutputData(Map.of(
                    "errorCode", ex.getErrorCode(),
                    "errorMessage", Objects.toString(ex.getMessage(), "")
            ));
            result.setReasonForIncompletion(ex.getErrorCode() + ": " + Objects.toString(ex.getMessage(), ""));
            result.setStatus(TaskResult.Status.FAILED);
            return result;
        } catch (Exception ex) {
            log.error("event=validate_reconcile_failed taskId={} workflowId={} message={}",
                    task.getTaskId(), task.getWorkflowInstanceId(), ex.getMessage(), ex);
            result.setOutputData(Map.of(
                    "errorCode", "UNEXPECTED_ERROR",
                    "errorMessage", Objects.toString(ex.getMessage(), "")
            ));
            result.setReasonForIncompletion("UNEXPECTED_ERROR: " + Objects.toString(ex.getMessage(), ""));
            result.setStatus(TaskResult.Status.FAILED);
            return result;
        }
    }

    private static String requireString(Map<String, Object> input, String key) {
        Object raw = input.get(key);
        if (raw == null || raw.toString().isBlank()) {
            throw new PipelineTaskException("INVALID_INPUT", "missing required input: " + key);
        }
        return raw.toString().trim();
    }
}
