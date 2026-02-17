package com.example.agents.pdfingestionservice.service;

import com.example.agents.pdfingestionservice.dto.PdfIngestionRequestDto;
import com.example.agents.pdfingestionservice.dto.PdfIngestionResultDto;

public interface PdfIngestionPipelineService {
    PdfIngestionResultDto process(PdfIngestionRequestDto requestDto);
}
