package com.example.agents.financialextractionworker.controller;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.common.enums.PipelineStatus;
import com.example.agents.financialextractionworker.dto.PipelineStepRequestDto;
import com.example.agents.financialextractionworker.facade.IPipelineFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineControllerTest {

    @Mock
    private IPipelineFacade pipelineFacade;

    @InjectMocks
    private PipelineController controller;

    @Test
    void process_returnsFacadeResponse() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        PipelineMessageDto response = PipelineMessageDto.builder()
                .jobId("job-1")
                .taskType("task")
                .status(PipelineStatus.PROCESSED)
                .build();
        when(pipelineFacade.process(request)).thenReturn(response);

        var result = controller.process(request);

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isSameAs(response);
        verify(pipelineFacade).process(request);
    }
}
