package com.av.agents.textextractionworker.service;

import com.av.agents.textextractionworker.dto.TextExtractionRequestDto;
import com.av.agents.textextractionworker.dto.TextExtractionResultDto;

public interface ITextExtractionPipelineService {
    TextExtractionResultDto process(TextExtractionRequestDto requestDto);
}
