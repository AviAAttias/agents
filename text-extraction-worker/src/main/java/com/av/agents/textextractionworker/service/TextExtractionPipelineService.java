package com.av.agents.textextractionworker.service;

import com.av.agents.textextractionworker.dto.TextExtractionRequestDto;
import com.av.agents.textextractionworker.dto.TextExtractionResultDto;

public interface TextExtractionPipelineService {
    TextExtractionResultDto process(TextExtractionRequestDto requestDto);
}
