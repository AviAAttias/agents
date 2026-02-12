package com.example.agents.notificationworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.notificationworker.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
