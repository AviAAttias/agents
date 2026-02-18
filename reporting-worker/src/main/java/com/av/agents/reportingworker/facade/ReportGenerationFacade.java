package com.av.agents.reportingworker.facade;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.reportingworker.dto.PipelineStepRequestDto;
import com.av.agents.reportingworker.service.ReportGenerationWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportGenerationFacade implements IPipelineFacade {
    private final ReportGenerationWorker reportGenerationWorker;

    @Override
    public PipelineMessageDto process(PipelineStepRequestDto requestDto) {
        return reportGenerationWorker.process(requestDto);
    }
}
