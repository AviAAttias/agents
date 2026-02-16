package com.example.agents.textextractionworker.service;

import com.example.agents.textextractionworker.dto.TextExtractionRequestDto;
import com.example.agents.textextractionworker.entity.PipelineStepEntity;
import com.example.agents.textextractionworker.entity.TextArtifactEntity;
import com.example.agents.textextractionworker.repository.IPipelineStepRepository;
import com.example.agents.textextractionworker.repository.ITextArtifactRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineStepServiceTest {

    @Mock
    private IPipelineStepRepository pipelineStepRepository;
    @Mock
    private ITextArtifactRepository textArtifactRepository;
    @Mock
    private PdfTextExtractionService textExtractionService;

    private TextExtractionPipelineStepService service;

    @BeforeEach
    void setup() {
        service = new TextExtractionPipelineStepService(pipelineStepRepository, textArtifactRepository, textExtractionService, new ObjectMapper());
    }

    @Test
    void process_returnsCachedOutputWhenCompletedExists() {
        TextExtractionRequestDto request = new TextExtractionRequestDto();
        request.setJobId("job-1");
        request.setArtifactRef("/tmp/file.pdf");

        PipelineStepEntity existing = new PipelineStepEntity();
        existing.setStatus("PROCESSED");
        existing.setOutputJson("{\"text\":\"cached\",\"artifactRef\":\"text-artifact://10\",\"textArtifact\":\"text-artifact://10\",\"inputBytes\":100,\"outputChars\":6,\"durationMs\":10,\"wasTruncated\":false,\"pageCount\":1,\"extractionMethod\":\"pdfbox\",\"sha256\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}");
        when(pipelineStepRepository.findByIdempotencyKey("job-1:extract_text")).thenReturn(Optional.of(existing));

        var output = service.process(request);

        assertThat(output.getText()).isEqualTo("cached");
        verify(textExtractionService, never()).extract(any());
        verify(textArtifactRepository, never()).save(any());
    }

    @Test
    void process_persistsArtifactsOnFirstRun() {
        TextExtractionRequestDto request = new TextExtractionRequestDto();
        request.setJobId("job-2");
        request.setArtifactRef("/tmp/file.pdf");
        when(pipelineStepRepository.findByIdempotencyKey("job-2:extract_text")).thenReturn(Optional.empty());
        when(textExtractionService.extract("/tmp/file.pdf")).thenReturn(new PdfTextExtractionService.ExtractionResult("text", 1, "pdfbox", 4, 10, "b".repeat(64), false));
        when(textArtifactRepository.save(any(TextArtifactEntity.class))).thenAnswer(i -> {
            TextArtifactEntity entity = i.getArgument(0);
            entity.setId(22L);
            return entity;
        });

        var output = service.process(request);

        assertThat(output.getArtifactRef()).isEqualTo("text-artifact://22");
        assertThat(output.getTextArtifact()).isEqualTo("text-artifact://22");
        verify(pipelineStepRepository).save(any(PipelineStepEntity.class));
    }
}
