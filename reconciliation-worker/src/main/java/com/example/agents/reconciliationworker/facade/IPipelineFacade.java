package com.example.agents.reconciliationworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.reconciliationworker.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
