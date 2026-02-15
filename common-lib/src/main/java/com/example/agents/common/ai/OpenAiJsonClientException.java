package com.example.agents.common.ai;

import lombok.Getter;

@Getter
public class OpenAiJsonClientException extends RuntimeException {
    private final OpenAiErrorCode errorCode;
    private final String schemaViolationSummary;

    public OpenAiJsonClientException(OpenAiErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public OpenAiJsonClientException(OpenAiErrorCode errorCode, String message, String schemaViolationSummary) {
        this(errorCode, message, schemaViolationSummary, null);
    }

    public OpenAiJsonClientException(OpenAiErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    public OpenAiJsonClientException(OpenAiErrorCode errorCode, String message, String schemaViolationSummary, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.schemaViolationSummary = schemaViolationSummary;
    }
}
