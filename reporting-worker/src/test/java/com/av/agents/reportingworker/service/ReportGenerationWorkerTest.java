package com.av.agents.reportingworker.service;

import com.av.agents.reportingworker.dto.PipelineStepRequestDto;
import com.av.agents.reportingworker.entity.ReportArtifactEntity;
import com.av.agents.reportingworker.repository.ReportArtifactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportGenerationWorkerTest {

    @Mock
    private ReportArtifactRepository reportArtifactRepository;
    @Mock
    private IReportGenerationService reportGenerationService;
    @InjectMocks
    private ReportGenerationWorker worker;

    @Test
    void process_isIdempotentForRetry() {
        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-3");
        request.setTaskType("generate_report");
        request.setPayloadJson("{}");

        ReportArtifactEntity existing = new ReportArtifactEntity();
        existing.setId(20L);
        existing.setJobId("job-3");
        existing.setTaskType("generate_report");
        existing.setArtifactRef("rep:20");
        existing.setFormat("MARKDOWN");
        existing.setContent("fixed");
        existing.setContentSha256("abc");
        existing.setCreatedAt(OffsetDateTime.now());

        when(reportArtifactRepository.findByJobIdAndTaskType("job-3", "generate_report"))
                .thenReturn(Optional.of(existing));

        var response = worker.process(request);

        assertThat(response.getArtifactRef()).isEqualTo("rep:20");
        verify(reportGenerationService, never()).generateMarkdownReport(any());
        verify(reportArtifactRepository, never()).save(any());
    }
}
