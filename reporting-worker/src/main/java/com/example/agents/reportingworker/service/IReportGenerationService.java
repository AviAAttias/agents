package com.example.agents.reportingworker.service;

import com.example.agents.reportingworker.dto.PipelineStepRequestDto;

public interface IReportGenerationService {
    String generateMarkdownReport(PipelineStepRequestDto requestDto);
}
