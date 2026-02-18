package com.av.agents.pdfingestionservice.service;

import com.av.agents.common.enums.PipelineStatus;
import com.av.agents.pdfingestionservice.dto.PdfIngestionRequestDto;
import com.av.agents.pdfingestionservice.dto.PdfIngestionResultDto;
import com.av.agents.pdfingestionservice.entity.PipelineStepEntity;
import com.av.agents.pdfingestionservice.repository.IPipelineStepRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfIngestionPipelineStepService implements IPdfIngestionPipelineService {
    private static final String TASK_TYPE = "ingest_pdf";

    private final IPipelineStepRepository pipelineStepRepository;
    private final IPdfIngestionService pdfIngestionService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PdfIngestionResultDto process(PdfIngestionRequestDto requestDto) {
        String idempotencyKey = requestDto.getJobId() + ":" + TASK_TYPE;
        PipelineStepEntity existing = pipelineStepRepository.findByIdempotencyKey(idempotencyKey).orElse(null);

        if (existing != null && PipelineStatus.PROCESSED.name().equals(existing.getStatus()) && existing.getOutputJson() != null) {
            log.info("event=ingest_pdf_cached jobId={} workflowId={} taskId={} taskType={}", requestDto.getJobId(), requestDto.getWorkflowId(), requestDto.getTaskId(), TASK_TYPE);
            return readOutput(existing.getOutputJson());
        }

        long startedAt = System.currentTimeMillis();
        IPdfIngestionService.IngestionPayload payload = pdfIngestionService.ingest(requestDto.getJobId(), requestDto.getPdfUrl());
        long durationMs = System.currentTimeMillis() - startedAt;

        PdfIngestionResultDto output = PdfIngestionResultDto.builder()
                .artifactRef(payload.artifactRef())
                .sha256(payload.sha256())
                .bytes(payload.bytes())
                .durationMs(durationMs)
                .build();

        PipelineStepEntity entity = existing == null ? new PipelineStepEntity() : existing;
        entity.setJobId(requestDto.getJobId());
        entity.setTaskType(TASK_TYPE);
        entity.setStatus(PipelineStatus.PROCESSED.name());
        entity.setArtifactRef(payload.artifactRef());
        entity.setPayloadJson(requestDto.getPdfUrl());
        entity.setOutputJson(writeOutput(output));
        entity.setIdempotencyKey(idempotencyKey);
        entity.setUpdatedAt(OffsetDateTime.now());
        pipelineStepRepository.save(entity);

        return output;
    }

    private PdfIngestionResultDto readOutput(String outputJson) {
        try {
            return objectMapper.readValue(outputJson, PdfIngestionResultDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize stored output", e);
        }
    }

    private String writeOutput(PdfIngestionResultDto output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize output", e);
        }
    }
}
