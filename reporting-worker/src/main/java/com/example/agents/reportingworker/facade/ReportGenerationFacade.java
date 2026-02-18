package com.example.agents.reportingworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.reportingworker.dto.PipelineStepRequestDto;
import com.example.agents.reportingworker.service.ReportGenerationWorker;
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
