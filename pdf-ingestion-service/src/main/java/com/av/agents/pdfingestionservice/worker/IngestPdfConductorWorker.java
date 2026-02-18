package com.av.agents.pdfingestionservice.worker;

import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.pdfingestionservice.dto.PdfIngestionRequestDto;
import com.av.agents.pdfingestionservice.dto.PdfIngestionResultDto;
import com.av.agents.pdfingestionservice.service.PdfIngestionPipelineService;
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
public class IngestPdfConductorWorker implements Worker {
    private static final String TASK_NAME = "ingest_pdf";

    private final PdfIngestionPipelineService pdfIngestionPipelineService;

    @Override
    public String getTaskDefName() {
        return TASK_NAME;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        try {
            PdfIngestionRequestDto request = PdfIngestionRequestDto.builder()
                    .jobId(String.valueOf(task.getInputData().get("jobId")))
                    .pdfUrl(String.valueOf(task.getInputData().get("pdfUrl")))
                    .workflowId(task.getWorkflowInstanceId())
                    .taskId(task.getTaskId())
                    .taskType(getTaskDefName())
                    .build();

            PdfIngestionResultDto output = pdfIngestionPipelineService.process(request);
            Map<String, Object> outputData = new HashMap<>();
            outputData.put("artifactRef", output.getArtifactRef());
            outputData.put("sha256", output.getSha256());
            outputData.put("bytes", output.getBytes());
            outputData.put("durationMs", output.getDurationMs());
            result.setOutputData(outputData);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        } catch (PipelineTaskException ex) {
            log.warn("event=ingest_pdf_failed taskType={} taskId={} workflowId={} errorCode={} message={}",
                    getTaskDefName(), task.getTaskId(), task.getWorkflowInstanceId(), ex.getErrorCode(), ex.getMessage());
            return failedResult(result, ex.getErrorCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("event=ingest_pdf_failed taskType={} taskId={} workflowId={} message={}",
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
