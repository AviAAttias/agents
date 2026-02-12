package com.example.agents.reportingworker.service;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.reportingworker.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
