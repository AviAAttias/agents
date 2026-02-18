package com.av.agents.reconciliationworker.service;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.reconciliationworker.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
