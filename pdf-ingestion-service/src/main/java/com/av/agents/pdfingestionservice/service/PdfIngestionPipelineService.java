package com.av.agents.pdfingestionservice.service;

import com.av.agents.pdfingestionservice.dto.PdfIngestionRequestDto;
import com.av.agents.pdfingestionservice.dto.PdfIngestionResultDto;

public interface PdfIngestionPipelineService {
    PdfIngestionResultDto process(PdfIngestionRequestDto requestDto);
}
