package com.example.agents.textextractionworker.service;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.textextractionworker.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
