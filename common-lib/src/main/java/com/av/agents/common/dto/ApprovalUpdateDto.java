package com.av.agents.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "Reviewer patch payload for extracted fields")
public class ApprovalUpdateDto {
    private String reviewer;
    private Map<String, Object> patchedValues;
    private String decision;
}
