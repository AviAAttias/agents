package com.example.agents.classificationworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.classificationworker.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
