package com.example.agents.pdfingestionservice.service;

import com.example.agents.common.ai.PipelineTaskException;
import com.example.agents.pdfingestionservice.repository.IPdfArtifactRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfIngestionServiceTest {

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
    void ingest_rejectsOverMaxBytes(@TempDir Path tempDir) {
        byte[] oversized = ("%PDF-" + "x".repeat(2048)).getBytes(StandardCharsets.UTF_8);
        registerBinary("/big.pdf", "application/pdf", oversized);

        IPdfArtifactRepository repository = mock(IPdfArtifactRepository.class);
        when(repository.findBySha256(any())).thenReturn(Optional.empty());

        PdfIngestionService service = new PdfIngestionService(repository);
        ReflectionTestUtils.setField(service, "maxBytes", 128L);
        ReflectionTestUtils.setField(service, "timeoutMs", 3000);
        ReflectionTestUtils.setField(service, "artifactsDir", tempDir.toString());

        String url = "http://localhost:" + server.getAddress().getPort() + "/big.pdf";
        assertThatThrownBy(() -> service.ingest("job-1", url))
                .isInstanceOf(PipelineTaskException.class)
                .hasMessageContaining("PDF exceeds max allowed bytes");
    }

    @Test
    void ingest_sha256IsStable(@TempDir Path tempDir) {
        byte[] pdfBytes = "%PDF-1.4\nhello\n%%EOF".getBytes(StandardCharsets.UTF_8);
        registerBinary("/sample.pdf", "application/pdf", pdfBytes);

        IPdfArtifactRepository repository = mock(IPdfArtifactRepository.class);
        when(repository.findBySha256(any())).thenReturn(Optional.empty());

        PdfIngestionService service = new PdfIngestionService(repository);
        ReflectionTestUtils.setField(service, "maxBytes", 4096L);
        ReflectionTestUtils.setField(service, "timeoutMs", 3000);
        ReflectionTestUtils.setField(service, "artifactsDir", tempDir.toString());

        String url = "http://localhost:" + server.getAddress().getPort() + "/sample.pdf";
        var first = service.ingest("job-1", url);
        var second = service.ingest("job-1", url);

        assertThat(first.sha256()).isEqualTo(second.sha256());
        assertThat(first.artifactRef()).isEqualTo("pdf:" + first.sha256());
    }

    private void registerBinary(String path, String contentType, byte[] bytes) {
        server.createContext(path, exchange -> writeResponse(exchange, contentType, bytes));
    }

    private void writeResponse(HttpExchange exchange, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
