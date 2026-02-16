package com.example.agents.textextractionworker.facade;

import com.example.agents.textextractionworker.dto.TextExtractionRequestDto;
import com.example.agents.textextractionworker.dto.TextExtractionResultDto;
import com.example.agents.textextractionworker.service.IPipelineStepService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineFacadeTest {

    @Mock
    private IPipelineStepService service;

    @InjectMocks
    private PipelineFacade facade;

    @Test
    void process_delegatesToService() {
        TextExtractionRequestDto request = new TextExtractionRequestDto();
        TextExtractionResultDto response = TextExtractionResultDto.builder().text("ok").artifactRef("a").textArtifact("a").build();
        when(service.process(request)).thenReturn(response);

        var result = facade.process(request);

        assertThat(result).isSameAs(response);
        verify(service).process(request);
    }
}
