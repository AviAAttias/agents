package com.av.agents.common.artifacts;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public record ArtifactRef(String scheme, String id, String raw) {
    private static final Set<String> ALLOWED_SCHEMES = Set.of(
            "file", "http", "https", "text-artifact", "fin", "val", "report", "appr", "artifact"
    );

    public static ArtifactRef parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ArtifactResolutionException("Artifact reference cannot be blank");
        }
        String normalized = raw.trim();

        if (normalized.startsWith("/")) {
            return new ArtifactRef("file", normalized, normalized);
        }

        if (!normalized.contains(":")) {
            return new ArtifactRef("file", normalized, normalized);
        }

        URI uri = URI.create(normalized);
        String parsedScheme = uri.getScheme();
        if (parsedScheme == null || parsedScheme.isBlank()) {
            throw new ArtifactResolutionException("Artifact reference is missing scheme: " + normalized);
        }

        String scheme = parsedScheme.toLowerCase(Locale.ROOT);
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new ArtifactResolutionException("Unsupported artifact reference scheme: " + scheme);
        }

        String id = switch (scheme) {
            case "file" -> uri.getPath();
            case "http", "https" -> normalized;
            default -> uri.getSchemeSpecificPart();
        };

        if (id == null || id.isBlank()) {
            throw new ArtifactResolutionException("Artifact reference is missing identifier: " + normalized);
        }

        return new ArtifactRef(scheme, id, normalized);
    }
}
