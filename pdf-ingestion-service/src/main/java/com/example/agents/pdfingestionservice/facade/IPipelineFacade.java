package com.example.agents.pdfingestionservice.facade;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.pdfingestionservice.dto.PipelineStepRequestDto;

public interface IPipelineFacade {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
