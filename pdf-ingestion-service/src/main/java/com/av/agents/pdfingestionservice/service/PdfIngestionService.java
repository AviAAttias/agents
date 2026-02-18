package com.av.agents.pdfingestionservice.service;

import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.pdfingestionservice.entity.PdfArtifactEntity;
import com.av.agents.pdfingestionservice.repository.IPdfArtifactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PdfIngestionService implements IPdfIngestionService {
    private final IPdfArtifactRepository pdfArtifactRepository;

    @Value("${pdf.ingestion.max-bytes:26214400}")
    private long maxBytes;

    @Value("${pdf.ingestion.timeout-ms:15000}")
    private int timeoutMs;

    @Value("${pdf.ingestion.artifacts-dir:artifacts}")
    private String artifactsDir;

    @Override
    public IngestionPayload ingest(String jobId, String pdfUrl) {
        DownloadedPdf downloadedPdf = download(pdfUrl);
        String sha256 = sha256Hex(downloadedPdf.bytes());
        String artifactRef = "pdf:" + sha256;

        if (pdfArtifactRepository.findBySha256(sha256).isPresent()) {
            return new IngestionPayload(sha256, downloadedPdf.bytes().length, artifactRef);
        }

        String relativePath = jobId + "/" + sha256 + ".pdf";
        Path destination = Path.of(artifactsDir).resolve(relativePath);
        writeArtifact(destination, downloadedPdf.bytes());

        PdfArtifactEntity entity = new PdfArtifactEntity();
        entity.setJobId(jobId);
        entity.setSha256(sha256);
        entity.setSourceUrl(pdfUrl);
        entity.setContentLength(downloadedPdf.bytes().length);
        entity.setStoragePath(destination.toString());
        entity.setCreatedAt(OffsetDateTime.now());
        pdfArtifactRepository.save(entity);

        return new IngestionPayload(sha256, downloadedPdf.bytes().length, artifactRef);
    }

    private DownloadedPdf download(String pdfUrl) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(pdfUrl))
                .GET()
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Accept", "application/pdf")
                .build();

        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                throw new PipelineTaskException("PDF_FETCH_FAILED", "Failed to fetch pdfUrl: HTTP " + response.statusCode());
            }
            response.headers().firstValue("Content-Type").ifPresent(value -> {
                String normalized = value.toLowerCase();
                if (!normalized.contains("application/pdf") && !normalized.contains("application/octet-stream")) {
                    throw new PipelineTaskException("PDF_INVALID_CONTENT_TYPE", "URL does not appear to be a PDF");
                }
            });
            byte[] bytes = readCapped(response.body());
            if (!looksLikePdf(bytes)) {
                throw new PipelineTaskException("PDF_INVALID_CONTENT", "Downloaded content is not a PDF");
            }
            return new DownloadedPdf(bytes);
        } catch (PipelineTaskException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PipelineTaskException("PDF_FETCH_FAILED", "Unable to download PDF: " + ex.getMessage(), ex);
        }
    }

    private byte[] readCapped(InputStream body) throws IOException {
        try (InputStream input = body; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new PipelineTaskException("PDF_TOO_LARGE", "PDF exceeds max allowed bytes: " + maxBytes);
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private boolean looksLikePdf(byte[] bytes) {
        return bytes.length >= 5
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F'
                && bytes[4] == '-';
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private void writeArtifact(Path destination, byte[] bytes) {
        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, bytes);
        } catch (IOException e) {
            throw new PipelineTaskException("ARTIFACT_WRITE_FAILED", "Unable to write artifact file", e);
        }
    }

    private record DownloadedPdf(byte[] bytes) {
    }
}
