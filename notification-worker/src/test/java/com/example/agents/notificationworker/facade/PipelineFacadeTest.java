package com.example.agents.notificationworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.notificationworker.dto.PipelineStepRequestDto;
import com.example.agents.notificationworker.worker.NotificationWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineFacadeTest {

    @Mock
    private NotificationWorker notificationWorker;

    @InjectMocks
    private NotificationFacade facade;

    @Test
    void process_delegatesToWorker() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        PipelineMessageDto expected = PipelineMessageDto.builder().jobId("job-1").build();
        when(notificationWorker.process(request)).thenReturn(expected);

        PipelineMessageDto result = facade.process(request);

        assertThat(result).isSameAs(expected);
        verify(notificationWorker).process(request);
    }
}
