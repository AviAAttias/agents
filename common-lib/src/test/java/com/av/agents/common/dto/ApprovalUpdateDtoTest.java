package com.av.agents.common.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalUpdateDtoTest {

    @Test
    void settersAndGetters_workForAllFields() {
        ApprovalUpdateDto dto = new ApprovalUpdateDto();
        dto.setReviewer("alice");
        dto.setDecision("APPROVED");
        dto.setPatchedValues(Map.of("total", 10));

        assertThat(dto.getReviewer()).isEqualTo("alice");
        assertThat(dto.getDecision()).isEqualTo("APPROVED");
        assertThat(dto.getPatchedValues()).containsEntry("total", 10);
    }
}
