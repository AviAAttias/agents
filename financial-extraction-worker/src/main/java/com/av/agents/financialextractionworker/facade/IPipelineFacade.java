package com.av.agents.financialextractionworker.facade;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.financialextractionworker.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
