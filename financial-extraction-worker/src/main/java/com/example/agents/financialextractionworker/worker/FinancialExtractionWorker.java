package com.example.agents.financialextractionworker.worker;

import com.example.agents.common.ai.PipelineTaskException;
import com.example.agents.financialextractionworker.dto.FinancialExtractionRequestDto;
import com.example.agents.financialextractionworker.dto.FinancialExtractionResultDto;
import com.example.agents.financialextractionworker.facade.FinancialExtractionFacade;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialExtractionWorker implements Worker {
    private static final String TASK_NAME = "extract_financials";

    private final FinancialExtractionFacade financialExtractionFacade;

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        try {
            FinancialExtractionRequestDto request = FinancialExtractionRequestDto.builder()
                    .jobId(String.valueOf(task.getInputData().get("jobId")))
                    .docType(String.valueOf(task.getInputData().get("docType")))
                    .text(String.valueOf(task.getInputData().get("text")))
                    .taskType(TASK_NAME)
                    .workflowId(task.getWorkflowInstanceId())
                    .taskId(task.getTaskId())
                    .build();

            FinancialExtractionResultDto extracted = financialExtractionFacade.extract(request);
            result.setOutputData(Map.of("artifactRef", extracted.getArtifactRef()));
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (PipelineTaskException ex) {
            log.warn("event=extract_financials_failed taskId={} workflowId={} errorCode={} message={}",
                    task.getTaskId(), task.getWorkflowInstanceId(), ex.getErrorCode(), ex.getMessage());
            result.setOutputData(Map.of("errorCode", ex.getErrorCode(), "errorMessage", ex.getMessage()));
            result.setReasonForIncompletion(ex.getErrorCode() + ": " + ex.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            return result;
        } catch (Exception ex) {
            log.error("event=extract_financials_failed taskId={} workflowId={} message={}",
                    task.getTaskId(), task.getWorkflowInstanceId(), ex.getMessage(), ex);
            result.setOutputData(Map.of("errorCode", "UNEXPECTED_ERROR", "errorMessage", ex.getMessage()));
            result.setReasonForIncompletion("UNEXPECTED_ERROR: " + ex.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            return result;
        }
    }
}
