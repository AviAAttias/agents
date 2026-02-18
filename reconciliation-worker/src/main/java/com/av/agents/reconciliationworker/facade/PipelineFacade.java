package com.av.agents.reconciliationworker.facade;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.reconciliationworker.dto.PipelineStepRequestDto;
import com.av.agents.reconciliationworker.service.IPipelineStepService;
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
