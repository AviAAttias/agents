package com.example.agents.approvalservice.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.approvalservice.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
