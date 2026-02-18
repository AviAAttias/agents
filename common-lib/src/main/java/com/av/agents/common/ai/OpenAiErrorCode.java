package com.av.agents.common.ai;

public enum OpenAiErrorCode {
    RATE_LIMIT,
    TIMEOUT,
    INVALID_SCHEMA_OUTPUT,
    UPSTREAM_5XX,
    AUTH_CONFIG,
    CLIENT_ERROR,
    DISABLED
}
