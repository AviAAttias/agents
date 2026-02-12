package com.example.agents.classificationworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.classificationworker.dto.PipelineStepRequestDto;
import com.example.agents.classificationworker.service.IPipelineStepService;
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
