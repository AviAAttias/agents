package com.av.agents.textextractionworker.service;

import com.av.agents.common.enums.PipelineStatus;
import com.av.agents.textextractionworker.dto.TextExtractionRequestDto;
import com.av.agents.textextractionworker.dto.TextExtractionResultDto;
import com.av.agents.textextractionworker.entity.PipelineStepEntity;
import com.av.agents.sharedpersistence.entity.TextArtifactEntity;
import com.av.agents.textextractionworker.repository.IPipelineStepRepository;
import com.av.agents.sharedpersistence.repository.ITextArtifactRepository;
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
public class TextExtractionPipelineStepService implements ITextExtractionPipelineService {
    private static final String TASK_TYPE = "extract_text";

    private final IPipelineStepRepository pipelineStepRepository;
    private final ITextArtifactRepository textArtifactRepository;
    private final IPdfTextExtractionService textExtractionService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TextExtractionResultDto process(TextExtractionRequestDto requestDto) {
        PipelineStepEntity existing = pipelineStepRepository.findByIdempotencyKey(requestDto.getJobId() + ":" + TASK_TYPE).orElse(null);
        if (existing != null && PipelineStatus.PROCESSED.name().equals(existing.getStatus()) && existing.getOutputJson() != null) {
            log.info("event=extract_text_cached jobId={} workflowId={} taskId={} taskType={}", requestDto.getJobId(), requestDto.getWorkflowId(), requestDto.getTaskId(), TASK_TYPE);
            return readOutput(existing.getOutputJson());
        }

        long startMs = System.currentTimeMillis();
        IPdfTextExtractionService.ExtractionResult extractionResult = textExtractionService.extract(requestDto.getArtifactRef());

        TextArtifactEntity artifact = new TextArtifactEntity();
        artifact.setJobId(requestDto.getJobId());
        artifact.setTextBody(extractionResult.extractedText());
        artifact.setWasTruncated(extractionResult.wasTruncated());
        artifact.setPageCount(extractionResult.pageCount());
        artifact.setInputBytes(extractionResult.inputBytes());
        artifact.setOutputChars(extractionResult.outputChars());
        artifact.setSha256(extractionResult.sha256());
        artifact.setExtractionMethod(extractionResult.extractionMethod());
        artifact.setCreatedAt(OffsetDateTime.now());
        textArtifactRepository.save(artifact);

        long durationMs = System.currentTimeMillis() - startMs;
        String textArtifactRef = "text-artifact://" + artifact.getId();
        TextExtractionResultDto output = TextExtractionResultDto.builder()
                .text(extractionResult.extractedText())
                .artifactRef(textArtifactRef)
                .textArtifact(textArtifactRef)
                .inputBytes(extractionResult.inputBytes())
                .outputChars(extractionResult.outputChars())
                .durationMs(durationMs)
                .wasTruncated(extractionResult.wasTruncated())
                .pageCount(extractionResult.pageCount())
                .extractionMethod(extractionResult.extractionMethod())
                .sha256(extractionResult.sha256())
                .build();

        PipelineStepEntity entity = existing == null ? new PipelineStepEntity() : existing;
        entity.setJobId(requestDto.getJobId());
        entity.setTaskType(TASK_TYPE);
        entity.setStatus(PipelineStatus.PROCESSED.name());
        entity.setArtifactRef(textArtifactRef);
        entity.setPayloadJson(requestDto.getArtifactRef());
        entity.setOutputJson(writeOutput(output));
        entity.setIdempotencyKey(requestDto.getJobId() + ":" + TASK_TYPE);
        entity.setUpdatedAt(OffsetDateTime.now());
        pipelineStepRepository.save(entity);

        log.info("event=extract_text_completed jobId={} workflowId={} taskId={} taskType={} inputBytes={} outputChars={} pageCount={} wasTruncated={} sha256={} durationMs={}",
                requestDto.getJobId(), requestDto.getWorkflowId(), requestDto.getTaskId(), TASK_TYPE,
                extractionResult.inputBytes(), extractionResult.outputChars(), extractionResult.pageCount(), extractionResult.wasTruncated(), extractionResult.sha256(), durationMs);
        return output;
    }

    private TextExtractionResultDto readOutput(String outputJson) {
        try {
            return objectMapper.readValue(outputJson, TextExtractionResultDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize stored output", e);
        }
    }

    private String writeOutput(TextExtractionResultDto output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize output", e);
        }
    }
}
