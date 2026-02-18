package com.av.agents.common.artifacts;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtifactResolverTest {

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void readBytes_readsFile(@TempDir Path tempDir) throws Exception {
        Path path = tempDir.resolve("sample.txt");
        Files.writeString(path, "hello");

        ArtifactResolver resolver = ArtifactResolver.defaultResolver(1000, 1000);
        byte[] bytes = resolver.readBytes(ArtifactRef.parse(path.toUri().toString()), 100);

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void readBytes_readsHttp() {
        server.createContext("/sample", exchange -> writeResponse(exchange, "ok"));
        String url = "http://localhost:" + server.getAddress().getPort() + "/sample";

        ArtifactResolver resolver = ArtifactResolver.defaultResolver(2000, 2000);
        byte[] bytes = resolver.readBytes(ArtifactRef.parse(url), 100);

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("ok");
    }

    @Test
    void readBytes_enforcesMaxBytes() {
        server.createContext("/big", exchange -> writeResponse(exchange, "0123456789"));
        String url = "http://localhost:" + server.getAddress().getPort() + "/big";

        ArtifactResolver resolver = ArtifactResolver.defaultResolver(2000, 2000);
        assertThatThrownBy(() -> resolver.readBytes(ArtifactRef.parse(url), 5))
                .isInstanceOf(ArtifactResolutionException.class)
                .hasMessageContaining("exceeds max bytes");
    }

    private void writeResponse(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
