package com.av.agents.textextractionworker.service;

import com.av.agents.common.ai.PipelineTaskException;
import com.av.agents.common.artifacts.ArtifactRef;
import com.av.agents.common.artifacts.ArtifactResolutionException;
import com.av.agents.common.artifacts.ArtifactResolver;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PdfBoxTextExtractionService implements PdfTextExtractionService {
    private final MeterRegistry meterRegistry;

    @Value("${TEXT_EXTRACTION_MAX_TEXT_CHARS:12000}")
    private int maxTextChars;

    @Value("${TEXT_EXTRACTION_MAX_INPUT_BYTES:26214400}")
    private long maxInputBytes;

    @Value("${TEXT_EXTRACTION_CONNECT_TIMEOUT_MS:5000}")
    private int connectTimeoutMs;

    @Value("${TEXT_EXTRACTION_READ_TIMEOUT_MS:10000}")
    private int readTimeoutMs;

    @Override
    public ExtractionResult extract(String artifactRef) {
        byte[] pdfBytes = loadPdfBytes(artifactRef);
        String sha256 = sha256(pdfBytes);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String extracted = new PDFTextStripper().getText(document);
            boolean truncated = extracted.length() > maxTextChars;
            String bounded = truncated ? extracted.substring(0, maxTextChars) : extracted;

            meterRegistry.counter("text_extraction_requests_total", "status", "success").increment();
            return new ExtractionResult(
                    bounded,
                    document.getNumberOfPages(),
                    "pdfbox",
                    bounded.length(),
                    pdfBytes.length,
                    sha256,
                    truncated
            );
        } catch (InvalidPasswordException ex) {
            meterRegistry.counter("text_extraction_requests_total", "status", "password_protected").increment();
            throw new PipelineTaskException("PDF_PASSWORD_PROTECTED", "PDF is password protected", ex);
        } catch (IOException ex) {
            meterRegistry.counter("text_extraction_requests_total", "status", "malformed").increment();
            throw new PipelineTaskException("PDF_MALFORMED", "PDF cannot be parsed", ex);
        }
    }

    private byte[] loadPdfBytes(String artifactRefRaw) {
        try {
            ArtifactResolver resolver = ArtifactResolver.defaultResolver(connectTimeoutMs, readTimeoutMs);
            ArtifactRef ref = ArtifactRef.parse(artifactRefRaw);
            return resolver.readBytes(ref, maxInputBytes);
        } catch (ArtifactResolutionException ex) {
            throw new PipelineTaskException("ARTIFACT_READ_FAILED", ex.getMessage(), ex);
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}
