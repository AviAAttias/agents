package com.av.agents.financialextractionworker.worker;

import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.financialextractionworker.dto.FinancialExtractionRequestDto;
import com.av.agents.financialextractionworker.dto.FinancialExtractionResultDto;
import com.av.agents.financialextractionworker.facade.FinancialExtractionFacade;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

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
        // Avoid TaskResult(Task) because it dereferences task.getStatus() which may be null in tests/clients.
        TaskResult result = new TaskResult();
        result.setTaskId(task.getTaskId());
        result.setWorkflowInstanceId(task.getWorkflowInstanceId());

        try {
            Map<String, Object> in = task.getInputData() == null ? Map.of() : task.getInputData();

            String jobId = requireString(in, "jobId");
            String docType = requireString(in, "docType");
            String text = requireString(in, "text");

            FinancialExtractionRequestDto request = FinancialExtractionRequestDto.builder()
                    .jobId(jobId)
                    .docType(docType)
                    .text(text)
                    .taskType(TASK_NAME)
                    .workflowId(task.getWorkflowInstanceId())
                    .taskId(task.getTaskId())
                    .build();

            FinancialExtractionResultDto extracted = financialExtractionFacade.extract(request);

            // Success contract: output contains exactly artifactRef
            result.setOutputData(Map.of("artifactRef", extracted.getArtifactRef()));
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;

        } catch (PipelineTaskException ex) {
            log.warn("event=extract_financials_failed taskId={} workflowId={} errorCode={} message={}",
                    task.getTaskId(), task.getWorkflowInstanceId(), ex.getErrorCode(), ex.getMessage());

            result.setOutputData(Map.of(
                    "errorCode", ex.getErrorCode(),
                    "errorMessage", Objects.toString(ex.getMessage(), "")
            ));
            result.setReasonForIncompletion(ex.getErrorCode() + ": " + Objects.toString(ex.getMessage(), ""));
            result.setStatus(TaskResult.Status.FAILED);
            return result;

        } catch (Exception ex) {
            log.error("event=extract_financials_failed taskId={} workflowId={} message={}",
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
        Object v = input.get(key);
        if (v == null) {
            throw new IllegalArgumentException("missing required input: " + key);
        }
        String s = v.toString().trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("blank required input: " + key);
        }
        return s;
    }
}
