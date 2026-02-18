package com.av.agents.classificationworker.service;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.classificationworker.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
