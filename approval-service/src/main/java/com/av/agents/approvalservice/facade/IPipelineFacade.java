package com.av.agents.approvalservice.facade;

import com.av.agents.common.dto.PipelineMessageDto;
import com.av.agents.approvalservice.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
