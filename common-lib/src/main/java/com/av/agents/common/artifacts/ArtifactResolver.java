package com.av.agents.common.artifacts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ArtifactResolver {
    private final ArtifactProperties properties;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public ArtifactResolver(
            ArtifactProperties properties,
            @Value("${artifacts.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${artifacts.read-timeout-ms:10000}") int readTimeoutMs
    ) {
        this.properties = properties;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public InputStream open(ArtifactRef ref) {
        return switch (ref.scheme()) {
            case "file" -> openFile(ref);
            case "http", "https" -> openHttp(ref);
            default -> throw new ArtifactResolutionException("Resolver unavailable for scheme: " + ref.scheme());
        };
    }

    public byte[] readBytes(ArtifactRef ref) {
        return readBytes(ref, properties.maxBytes());
    }

    public byte[] readBytes(ArtifactRef ref, long maxBytes) {
        long effectiveMaxBytes = maxBytes > 0 ? Math.min(maxBytes, properties.maxBytes()) : properties.maxBytes();
        try (InputStream input = open(ref);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > effectiveMaxBytes) {
                    throw new ArtifactResolutionException("Artifact exceeds max bytes: " + effectiveMaxBytes);
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ArtifactResolutionException("Could not read artifact bytes", ex);
        }
    }

    public String readText(ArtifactRef ref) {
        return new String(readBytes(ref), StandardCharsets.UTF_8);
    }

    public String readText(ArtifactRef ref, long maxBytes) {
        return new String(readBytes(ref, maxBytes), StandardCharsets.UTF_8);
    }

    public JsonNode readJson(ArtifactRef ref, ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(readBytes(ref));
        } catch (IOException ex) {
            throw new ArtifactResolutionException("Could not parse artifact JSON", ex);
        }
    }

    public JsonNode readJson(ArtifactRef ref, long maxBytes, ObjectMapper objectMapper) {
        try {
            return objectMapper.readTree(readBytes(ref, maxBytes));
        } catch (IOException ex) {
            throw new ArtifactResolutionException("Could not parse artifact JSON", ex);
        }
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
