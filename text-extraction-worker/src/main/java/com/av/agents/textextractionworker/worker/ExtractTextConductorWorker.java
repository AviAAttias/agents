package com.av.agents.textextractionworker.worker;

import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.textextractionworker.dto.TextExtractionRequestDto;
import com.av.agents.textextractionworker.dto.TextExtractionResultDto;
import com.av.agents.textextractionworker.service.ITextExtractionPipelineService;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExtractTextConductorWorker implements Worker {
    private static final String TASK_NAME = "extract_text";

    private final ITextExtractionPipelineService textExtractionPipelineService;

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        try {
            String jobId = String.valueOf(task.getInputData().get("jobId"));
            String artifactRef = String.valueOf(task.getInputData().get("artifact"));

            TextExtractionRequestDto request = new TextExtractionRequestDto();
            request.setJobId(jobId);
            request.setArtifactRef(artifactRef);
            request.setWorkflowId(task.getWorkflowInstanceId());
            request.setTaskId(task.getTaskId());
            request.setTaskType(getTaskDefName());

            TextExtractionResultDto extracted = textExtractionPipelineService.process(request);
            Map<String, Object> outputData = new HashMap<>();
            outputData.put("text", extracted.getText());
            outputData.put("artifactRef", extracted.getArtifactRef());
            outputData.put("textArtifact", extracted.getTextArtifact());
            outputData.put("inputBytes", extracted.getInputBytes());
            outputData.put("outputChars", extracted.getOutputChars());
            outputData.put("durationMs", extracted.getDurationMs());
            outputData.put("wasTruncated", extracted.isWasTruncated());
            outputData.put("pageCount", extracted.getPageCount());
            result.setOutputData(outputData);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (PipelineTaskException ex) {
            log.warn("event=extract_text_failed taskType={} taskId={} workflowId={} errorCode={} message={}",
                    getTaskDefName(), task.getTaskId(), task.getWorkflowInstanceId(), ex.getErrorCode(), ex.getMessage());
            return failedResult(result, ex.getErrorCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("event=extract_text_failed taskType={} taskId={} workflowId={} message={}",
                    getTaskDefName(), task.getTaskId(), task.getWorkflowInstanceId(), ex.getMessage(), ex);
            return failedResult(result, "UNEXPECTED_ERROR", ex.getMessage());
        }
    }

    private TaskResult failedResult(TaskResult result, String errorCode, String errorMessage) {
        result.setOutputData(Map.of("errorCode", errorCode, "errorMessage", errorMessage));
        result.setReasonForIncompletion(errorCode + ": " + errorMessage);
        result.setStatus(TaskResult.Status.FAILED);
        return result;
    }
}
