package com.example.agents.notificationworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.notificationworker.dto.PipelineStepRequestDto;
import com.example.agents.notificationworker.service.IPipelineStepService;
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
