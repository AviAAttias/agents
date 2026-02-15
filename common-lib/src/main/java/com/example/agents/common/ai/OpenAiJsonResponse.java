package com.example.agents.common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OpenAiJsonResponse {
    JsonNode content;
    boolean inputTruncated;
    int inputChars;
    int outputChars;
    Integer tokensIn;
    Integer tokensOut;
}
