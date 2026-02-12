package com.example.agents.approvalservice.service;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.approvalservice.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
