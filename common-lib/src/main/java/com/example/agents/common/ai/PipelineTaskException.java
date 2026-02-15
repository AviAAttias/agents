package com.example.agents.common.ai;

import lombok.Getter;

@Getter
public class PipelineTaskException extends RuntimeException {
    private final String errorCode;

    public PipelineTaskException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PipelineTaskException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
