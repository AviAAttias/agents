package com.example.agents.classificationworker.service;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.classificationworker.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
