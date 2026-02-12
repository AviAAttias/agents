package com.example.agents.notificationworker.service;

import com.example.agents.notificationworker.dto.PipelineStepRequestDto;
import com.example.agents.notificationworker.entity.PipelineStepEntity;
import com.example.agents.notificationworker.mapper.IPipelineStepMapper;
import com.example.agents.notificationworker.repository.IPipelineStepRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private IPipelineStepRepository repository;
    @Mock private IPipelineStepMapper mapper;
    @Mock private JavaMailSender mailSender;

    @InjectMocks private NotificationService service;

    @Test
    void process_sendsEmailWhenEntityDoesNotExist() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-1");
        request.setPayloadJson("body");

        PipelineStepEntity mapped = new PipelineStepEntity();
        mapped.setJobId("job-1");
        mapped.setArtifactRef("artifact");

        when(repository.findByIdempotencyKey("job-1:send_email")).thenReturn(Optional.empty());
        when(mapper.toEntity(request)).thenReturn(mapped);

        var result = service.process(request);

        assertThat(result.getTaskType()).isEqualTo("send_email");
        assertThat(result.getPayloadJson()).isEqualTo("body");
        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
        verify(repository).save(mapped);
    }

    @Test
    void process_skipsEmailWhenEntityAlreadyExists() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-1");
        request.setPayloadJson("new");

        PipelineStepEntity existing = new PipelineStepEntity();
        existing.setJobId("job-1");
        existing.setTaskType("send_email");

        when(repository.findByIdempotencyKey("job-1:send_email")).thenReturn(Optional.of(existing));

        service.process(request);

        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
        verify(mapper, never()).toEntity(any());
        verify(repository).save(existing);
        assertThat(existing.getPayloadJson()).isEqualTo("new");
    }
}
