package com.av.agents.common.artifacts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public interface ArtifactResolver {
    InputStream open(ArtifactRef ref);

    default byte[] readBytes(ArtifactRef ref, long maxBytes) {
        try (InputStream input = open(ref);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new ArtifactResolutionException("Artifact exceeds max bytes: " + maxBytes);
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ArtifactResolutionException("Could not read artifact bytes", ex);
        }
    }

    default String readText(ArtifactRef ref, long maxBytes) {
        return new String(readBytes(ref, maxBytes), StandardCharsets.UTF_8);
    }

    default JsonNode readJson(ArtifactRef ref, long maxBytes, ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(readBytes(ref, maxBytes));
        } catch (IOException ex) {
            throw new ArtifactResolutionException("Could not parse artifact JSON", ex);
        }
    }

    static ArtifactResolver defaultResolver(int connectTimeoutMs, int readTimeoutMs) {
        return new DefaultArtifactResolver(connectTimeoutMs, readTimeoutMs);
    }

    class DefaultArtifactResolver implements ArtifactResolver {
        private final int connectTimeoutMs;
        private final int readTimeoutMs;

        public DefaultArtifactResolver(int connectTimeoutMs, int readTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            this.readTimeoutMs = readTimeoutMs;
        }

        @Override
        public InputStream open(ArtifactRef ref) {
            return switch (ref.scheme()) {
                case "file" -> openFile(ref);
                case "http", "https" -> openHttp(ref);
                default -> throw new ArtifactResolutionException("Resolver unavailable for scheme: " + ref.scheme());
            };
        }

        private InputStream openFile(ArtifactRef ref) {
            try {
                if (ref.raw().startsWith("file://")) {
                    return Files.newInputStream(Path.of(URI.create(ref.raw())));
                }
                return Files.newInputStream(Path.of(ref.raw()));
            } catch (IOException ex) {
                throw new ArtifactResolutionException("Could not open file artifact: " + ref.raw(), ex);
            }
        }

        private InputStream openHttp(ArtifactRef ref) {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(ref.raw()).toURL().openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(connectTimeoutMs);
                connection.setReadTimeout(readTimeoutMs);
                connection.setRequestProperty("Accept", "application/octet-stream,application/pdf,text/plain,application/json");
                int status = connection.getResponseCode();
                if (status >= 400) {
                    throw new ArtifactResolutionException("HTTP artifact fetch failed with status: " + status);
                }
                return connection.getInputStream();
            } catch (IOException ex) {
                throw new ArtifactResolutionException("Could not open http artifact: " + ref.raw(), ex);
            }
        }
    }
}
