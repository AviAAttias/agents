package com.av.agents.approvalservice.service;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.approvalservice.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
