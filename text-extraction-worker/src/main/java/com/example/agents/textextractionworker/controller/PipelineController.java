package com.example.agents.textextractionworker.controller;

import com.example.agents.textextractionworker.dto.TextExtractionRequestDto;
import com.example.agents.textextractionworker.dto.TextExtractionResultDto;
import com.example.agents.textextractionworker.facade.IPipelineFacade;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController {
    private final IPipelineFacade pipelineFacade;

    @PostMapping("/process")
    @CircuitBreaker(name = "pipelineWorker")
    public ResponseEntity<TextExtractionResultDto> process(@Valid @RequestBody TextExtractionRequestDto requestDto) {
        return ResponseEntity.ok(pipelineFacade.process(requestDto));
    }
}
