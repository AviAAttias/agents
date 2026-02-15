package com.example.agents.common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OpenAiJsonRequest {
    String operation;
    String schemaName;
    String systemPrompt;
    String userPrompt;
    JsonNode jsonSchema;
    String requestId;
    String jobId;
    String workflowId;
    String taskId;
    Integer maxInputChars;
    Integer maxOutputTokens;
}
