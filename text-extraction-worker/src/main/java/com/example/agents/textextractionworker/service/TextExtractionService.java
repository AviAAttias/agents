package com.example.agents.textextractionworker.service;

import com.example.agents.common.ai.PipelineTaskException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextExtractionService implements ITextExtractionService {
    private final MeterRegistry meterRegistry;

    @Value("${TEXT_EXTRACTION_MAX_TEXT_CHARS:12000}")
    private int maxTextChars;

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

    private byte[] loadPdfBytes(String artifactRef) {
        try {
            if (artifactRef.startsWith("file://")) {
                return Files.readAllBytes(Path.of(URI.create(artifactRef)));
            }
            if (artifactRef.startsWith("http://") || artifactRef.startsWith("https://")) {
                try (InputStream is = URI.create(artifactRef).toURL().openStream()) {
                    return is.readAllBytes();
                }
            }
            Path path = Path.of(artifactRef);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
            throw new PipelineTaskException(
                    "ARTIFACT_NOT_FOUND",
                    "Artifact reference is unsupported or does not exist: " + artifactRef
            );
        } catch (IOException ex) {
            throw new PipelineTaskException("ARTIFACT_READ_FAILED", "Could not read artifact bytes", ex);
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
