package com.av.agents.notificationworker.facade;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.notificationworker.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
