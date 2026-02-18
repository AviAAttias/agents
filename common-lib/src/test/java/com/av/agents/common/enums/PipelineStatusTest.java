package com.av.agents.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineStatusTest {

    @Test
    void enumContainsAllExpectedValues() {
        assertThat(PipelineStatus.values())
                .containsExactly(PipelineStatus.RECEIVED, PipelineStatus.PROCESSED, PipelineStatus.APPROVED, PipelineStatus.REJECTED, PipelineStatus.FAILED);
    }
}
