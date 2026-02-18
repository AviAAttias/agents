package com.av.agents.reportingworker.facade;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.reportingworker.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
