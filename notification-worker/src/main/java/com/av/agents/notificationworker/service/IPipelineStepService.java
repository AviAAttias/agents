package com.av.agents.notificationworker.service;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.notificationworker.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
