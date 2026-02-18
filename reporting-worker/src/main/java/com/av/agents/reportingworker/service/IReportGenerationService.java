package com.av.agents.reportingworker.service;

import com.av.agents.reportingworker.dto.PipelineStepRequestDto;

public interface IReportGenerationService {
    String generateMarkdownReport(PipelineStepRequestDto requestDto);
}
