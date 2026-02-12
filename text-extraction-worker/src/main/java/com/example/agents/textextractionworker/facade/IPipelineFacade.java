package com.example.agents.textextractionworker.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.textextractionworker.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
