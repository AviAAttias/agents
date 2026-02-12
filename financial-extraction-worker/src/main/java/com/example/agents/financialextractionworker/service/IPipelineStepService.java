package com.example.agents.financialextractionworker.service;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.financialextractionworker.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
