package com.example.agents.reconciliationworker.service;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.reconciliationworker.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
