package com.example.agents.common.dto;

import com.example.agents.common.enums.PipelineStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineMessageDtoTest {

    @Test
    void builder_setsAllFields() {
        OffsetDateTime now = OffsetDateTime.now();
        PipelineMessageDto dto = PipelineMessageDto.builder()
                .jobId("job")
                .taskType("type")
                .status(PipelineStatus.PROCESSED)
                .artifactRef("artifact")
                .payloadJson("payload")
                .processedAt(now)
                .build();

        assertThat(dto.getJobId()).isEqualTo("job");
        assertThat(dto.getTaskType()).isEqualTo("type");
        assertThat(dto.getStatus()).isEqualTo(PipelineStatus.PROCESSED);
        assertThat(dto.getArtifactRef()).isEqualTo("artifact");
        assertThat(dto.getPayloadJson()).isEqualTo("payload");
        assertThat(dto.getProcessedAt()).isEqualTo(now);
    }
}
