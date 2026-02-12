package com.example.agents.textextractionworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.textextractionworker.dto.PipelineStepRequestDto;
import com.example.agents.textextractionworker.service.IPipelineStepService;
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
