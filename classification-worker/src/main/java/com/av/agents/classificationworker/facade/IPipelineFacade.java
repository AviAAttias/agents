package com.av.agents.classificationworker.facade;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.classificationworker.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
