package com.example.agents.reportingworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.reportingworker.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
