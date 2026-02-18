package com.av.agents.financialextractionworker.service;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.financialextractionworker.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
