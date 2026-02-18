package com.av.agents.common.artifacts;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "artifacts")
public record ArtifactProperties(long maxBytes) {
    public ArtifactProperties() {
        this(10 * 1024 * 1024L);
    }
}
