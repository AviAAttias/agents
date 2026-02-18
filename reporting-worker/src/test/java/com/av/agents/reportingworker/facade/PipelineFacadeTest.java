package com.av.agents.reportingworker.facade;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.reportingworker.dto.PipelineStepRequestDto;
import com.av.agents.reportingworker.service.ReportGenerationWorker;
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
    private ReportGenerationWorker reportGenerationWorker;

    @InjectMocks
    private ReportGenerationFacade facade;

    @Test
    void process_delegatesToWorker() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        PipelineMessageDto expected = PipelineMessageDto.builder().jobId("job-1").build();
        when(reportGenerationWorker.process(request)).thenReturn(expected);

        PipelineMessageDto result = facade.process(request);

        assertThat(result).isSameAs(expected);
        verify(reportGenerationWorker).process(request);
    }
}
