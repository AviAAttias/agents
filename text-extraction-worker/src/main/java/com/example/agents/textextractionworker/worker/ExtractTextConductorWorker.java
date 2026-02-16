package com.example.agents.textextractionworker.worker;

import com.example.agents.common.ai.PipelineTaskException;
import com.example.agents.textextractionworker.dto.TextExtractionRequestDto;
import com.example.agents.textextractionworker.dto.TextExtractionResultDto;
import com.example.agents.textextractionworker.service.IPipelineStepService;
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
    private final IPipelineStepService pipelineStepService;

    @Override
    public String getTaskDefName() {
        return "extract_text";
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

            TextExtractionResultDto extracted = pipelineStepService.process(request);
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
            result.setReasonForIncompletion(ex.getErrorCode() + ": " + ex.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            return result;
        } catch (Exception ex) {
            log.error("event=extract_text_failed taskType={} taskId={} workflowId={} message={}",
                    getTaskDefName(), task.getTaskId(), task.getWorkflowInstanceId(), ex.getMessage(), ex);
            result.setReasonForIncompletion("UNEXPECTED_ERROR: " + ex.getMessage());
            result.setStatus(TaskResult.Status.FAILED);
            return result;
        }
    }
}
