package com.av.agents.reportingworker.service;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.reportingworker.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
