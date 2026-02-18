package com.av.agents.common.artifacts;

public class ArtifactResolutionException extends RuntimeException {
    public ArtifactResolutionException(String message) {
        super(message);
    }

    public ArtifactResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
