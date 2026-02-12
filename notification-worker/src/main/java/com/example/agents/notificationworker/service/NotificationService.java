package com.example.agents.notificationworker.service;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.notificationworker.dto.PipelineStepRequestDto;
import com.example.agents.notificationworker.entity.PipelineStepEntity;
import com.example.agents.notificationworker.mapper.IPipelineStepMapper;
import com.example.agents.notificationworker.repository.IPipelineStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService implements IPipelineStepService {

    private final IPipelineStepRepository pipelineStepRepository;
    private final IPipelineStepMapper pipelineStepMapper;
    private final JavaMailSender mailSender;

    @Override
    @Transactional
    public PipelineMessageDto process(PipelineStepRequestDto requestDto) {
        String idempotencyKey = requestDto.getJobId() + ":send_email";
        PipelineStepEntity entity = pipelineStepRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (entity == null) {
            entity = pipelineStepMapper.toEntity(requestDto);
            entity.setTaskType("send_email");
            entity.setIdempotencyKey(idempotencyKey);
            sendMail(requestDto.getJobId(), requestDto.getPayloadJson());
        }
        entity.setPayloadJson(requestDto.getPayloadJson());
        entity.setUpdatedAt(OffsetDateTime.now());
        pipelineStepRepository.save(entity);
        return PipelineMessageDto.builder().jobId(entity.getJobId()).taskType(entity.getTaskType()).artifactRef(entity.getArtifactRef()).payloadJson(entity.getPayloadJson()).processedAt(entity.getUpdatedAt()).build();
    }

    private void sendMail(String jobId, String payloadJson) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("reviewers@example.com");
        message.setSubject("Financial Pipeline Approval - " + jobId);
        message.setText(payloadJson);
        mailSender.send(message);
    }
}
