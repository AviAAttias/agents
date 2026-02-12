package com.example.agents.pdfingestionservice.service;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.pdfingestionservice.dto.PipelineStepRequestDto;

public interface IPipelineStepService {
    PipelineMessageDto process(PipelineStepRequestDto requestDto);
}
