package com.example.agents.approvalservice.controller;

import com.example.agents.common.dto.PipelineMessageDto;
import com.example.agents.approvalservice.dto.PipelineStepRequestDto;
import com.example.agents.approvalservice.facade.IPipelineFacade;
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
    public ResponseEntity<PipelineMessageDto> process(@Valid @RequestBody PipelineStepRequestDto requestDto) {
        return ResponseEntity.ok(pipelineFacade.process(requestDto));
    }
}
