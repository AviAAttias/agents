package com.example.agents.reportingworker.service;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.common.enums.PipelineStatus;
import com.example.agents.reportingworker.dto.PipelineStepRequestDto;
import com.example.agents.reportingworker.entity.ReportArtifactEntity;
import com.example.agents.reportingworker.repository.ReportArtifactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class ReportGenerationWorker {
    private final ReportArtifactRepository reportArtifactRepository;
    private final IReportGenerationService reportGenerationService;

    @Transactional
    public PipelineMessageDto process(PipelineStepRequestDto requestDto) {
        long startedAt = System.currentTimeMillis();
        return reportArtifactRepository.findByJobIdAndTaskType(requestDto.getJobId(), requestDto.getTaskType())
                .map(existing -> toMessage(existing, startedAt))
                .orElseGet(() -> createAndPersistReport(requestDto, startedAt));
    }

    private PipelineMessageDto createAndPersistReport(PipelineStepRequestDto requestDto, long startedAt) {
        String content = reportGenerationService.generateMarkdownReport(requestDto);
        String hash = sha256(content);

        ReportArtifactEntity entity = new ReportArtifactEntity();
        entity.setJobId(requestDto.getJobId());
        entity.setTaskType(requestDto.getTaskType());
        entity.setFormat("MARKDOWN");
        entity.setContent(content);
        entity.setContentSha256(hash);
        entity.setCreatedAt(OffsetDateTime.now());

        ReportArtifactEntity saved = reportArtifactRepository.save(entity);
        saved.setArtifactRef("rep:" + saved.getId());
        reportArtifactRepository.save(saved);

        return toMessage(saved, startedAt);
    }

    private PipelineMessageDto toMessage(ReportArtifactEntity entity, long startedAt) {
        long duration = System.currentTimeMillis() - startedAt;
        String outputPayload = String.format(
                "{\"reportFormat\":\"%s\",\"reportHash\":\"%s\",\"durationMs\":%d}",
                entity.getFormat(),
                entity.getContentSha256(),
                duration
        );
        return PipelineMessageDto.builder()
                .jobId(entity.getJobId())
                .taskType(entity.getTaskType())
                .status(PipelineStatus.PROCESSED)
                .artifactRef(entity.getArtifactRef())
                .payloadJson(outputPayload)
                .processedAt(OffsetDateTime.now())
                .build();
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte hashByte : hashBytes) {
                hex.append(String.format("%02x", hashByte));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to calculate SHA-256", e);
        }
    }
}
