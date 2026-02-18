package com.av.agents.classificationworker.service;

import com.av.agents.classificationworker.dto.PipelineStepRequestDto;
import com.av.agents.sharedpersistence.entity.TextArtifactEntity;
import com.av.agents.classificationworker.repository.IClassificationArtifactRepository;
import com.av.agents.classificationworker.repository.IPipelineStepRepository;
import com.av.agents.sharedpersistence.repository.ITextArtifactRepository;
import com.av.agents.common.ai.OpenAiJsonClient;
import com.av.agents.common.ai.OpenAiJsonRequest;
import com.av.agents.common.ai.OpenAiJsonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.config.import=",
        "conductor.enabled=false"
})
class PipelineStepServiceIntegrationTest {

    @Autowired
    private PipelineStepService pipelineStepService;

    @Autowired
    private ITextArtifactRepository textArtifactRepository;

    @Autowired
    private IPipelineStepRepository pipelineStepRepository;

    @Autowired
    private IClassificationArtifactRepository classificationArtifactRepository;

    @MockBean
    private OpenAiJsonClient openAiJsonClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        classificationArtifactRepository.deleteAll();
        pipelineStepRepository.deleteAll();
        textArtifactRepository.deleteAll();
    }

    @Test
    void returnsDocumentTypeAndCachesByJobAndTask() throws Exception {
        TextArtifactEntity artifact = new TextArtifactEntity();
        artifact.setId(101L);
        artifact.setTextBody("invoice text with subtotal and due date");
        textArtifactRepository.save(artifact);

        when(openAiJsonClient.completeJson(any(OpenAiJsonRequest.class)))
                .thenReturn(OpenAiJsonResponse.builder()
                        .content(objectMapper.readTree("{\"documentType\":\"invoice\"}"))
                        .outputChars(26)
                        .build());

        PipelineStepRequestDto request = new PipelineStepRequestDto();
        request.setJobId("job-cache");
        request.setTaskType("classify_doc");
        request.setPayloadJson("{\"textArtifact\":\"text-artifact://101\"}");

        String first = pipelineStepService.process(request).getPayloadJson();
        String second = pipelineStepService.process(request).getPayloadJson();

        assertThat(first).contains("\"documentType\":\"invoice\"");
        assertThat(second).contains("\"documentType\":\"invoice\"");
        verify(openAiJsonClient, times(1)).completeJson(any(OpenAiJsonRequest.class));
    }
}
