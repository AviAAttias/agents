package com.example.agents.financialextractionworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.financialextractionworker.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
