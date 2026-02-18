package com.av.agents.reconciliationworker.facade;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.reconciliationworker.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
