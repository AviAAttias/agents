package com.example.agents.textextractionworker.service;

import com.example.agents.textextractionworker.dto.TextExtractionRequestDto;
import com.example.agents.textextractionworker.dto.TextExtractionResultDto;

public interface IPipelineStepService {
    TextExtractionResultDto process(TextExtractionRequestDto requestDto);
}
