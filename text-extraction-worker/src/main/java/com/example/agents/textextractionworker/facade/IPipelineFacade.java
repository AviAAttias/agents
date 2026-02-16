package com.example.agents.textextractionworker.facade;

import com.example.agents.textextractionworker.dto.TextExtractionRequestDto;
import com.example.agents.textextractionworker.dto.TextExtractionResultDto;

public interface IPipelineFacade {
    TextExtractionResultDto process(TextExtractionRequestDto requestDto);
}
