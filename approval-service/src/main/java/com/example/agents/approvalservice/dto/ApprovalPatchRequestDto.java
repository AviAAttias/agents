package com.example.agents.approvalservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class ApprovalPatchRequestDto {
    @NotBlank
    private String decision;
    private String reviewer;
    private Map<String, Object> patchedValues;
}
