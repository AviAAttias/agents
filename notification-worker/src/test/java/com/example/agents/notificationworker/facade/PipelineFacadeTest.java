package com.example.agents.notificationworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.notificationworker.dto.PipelineStepRequestDto;
import com.example.agents.notificationworker.service.IPipelineStepService;
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
    private IPipelineStepService pipelineStepService;

    @InjectMocks
    private PipelineFacade facade;

    @Test
    void process_delegatesToPipelineStepService() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        PipelineMessageDto expected = PipelineMessageDto.builder().jobId("job-1").build();
        when(pipelineStepService.process(request)).thenReturn(expected);

        PipelineMessageDto result = facade.process(request);

        assertThat(result).isSameAs(expected);
        verify(pipelineStepService).process(request);
    }
}
