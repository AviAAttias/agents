package com.example.agents.textextractionworker.facade;

import com.example.agents.textextractionworker.dto.TextExtractionRequestDto;
import com.example.agents.textextractionworker.dto.TextExtractionResultDto;
import com.example.agents.textextractionworker.service.IPipelineStepService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PipelineFacade implements IPipelineFacade {
    private final IPipelineStepService pipelineStepService;

    @Override
    public TextExtractionResultDto process(TextExtractionRequestDto requestDto) {
        return pipelineStepService.process(requestDto);
    }
}
