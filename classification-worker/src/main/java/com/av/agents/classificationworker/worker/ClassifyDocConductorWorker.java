package com.av.agents.classificationworker.worker;

import com.av.agents.classificationworker.dto.PipelineStepRequestDto;
import com.av.agents.classificationworker.service.IPipelineStepService;
import com.av.agents.common.ai.PipelineTaskException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ClassifyDocConductorWorker implements Worker {
    private static final String TASK_NAME = "classify_doc";

    private final IPipelineStepService pipelineStepService;
    private final ObjectMapper objectMapper;

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        try {
            String jobId = String.valueOf(task.getInputData().get("jobId"));
            String textArtifact = String.valueOf(task.getInputData().get("textArtifact"));

            PipelineStepRequestDto request = new PipelineStepRequestDto();
            request.setJobId(jobId);
            request.setTaskType(getTaskDefName());
            request.setPayloadJson(objectMapper.createObjectNode().put("textArtifact", textArtifact).toString());

            JsonNode output = objectMapper.readTree(pipelineStepService.process(request).getPayloadJson());
            Map<String, Object> outputData = new HashMap<>();
            outputData.put("documentType", output.path("documentType").asText());
            outputData.put("model", output.path("model").asText(null));
            outputData.put("durationMs", output.path("durationMs").asLong());
            outputData.put("inputChars", output.path("inputChars").asInt());
            outputData.put("outputChars", output.path("outputChars").asInt());
            outputData.put("schemaName", output.path("schemaName").asText(null));
            if (!output.path("requestId").isMissingNode()) {
                outputData.put("requestId", output.path("requestId").asText(null));
            }

            result.setOutputData(outputData);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (PipelineTaskException ex) {
            log.warn("event=classify_doc_failed taskId={} workflowId={} errorCode={} message={}",
                    task.getTaskId(), task.getWorkflowInstanceId(), ex.getErrorCode(), ex.getMessage());
            return failedResult(result, ex.getErrorCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("event=classify_doc_failed taskId={} workflowId={} message={}",
                    task.getTaskId(), task.getWorkflowInstanceId(), ex.getMessage(), ex);
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
