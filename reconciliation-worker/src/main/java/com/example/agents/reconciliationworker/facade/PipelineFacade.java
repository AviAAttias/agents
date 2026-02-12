package com.example.agents.reconciliationworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.reconciliationworker.dto.PipelineStepRequestDto;
import com.example.agents.reconciliationworker.service.IPipelineStepService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PipelineFacade implements IPipelineFacade {
    private final IPipelineStepService pipelineStepService;

    @Override
    public PipelineMessageDto process(PipelineStepRequestDto requestDto) {
        return pipelineStepService.process(requestDto);
    }
}
