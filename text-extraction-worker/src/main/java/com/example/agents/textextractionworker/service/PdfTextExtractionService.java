package com.example.agents.textextractionworker.service;

public interface PdfTextExtractionService {
    ExtractionResult extract(String artifactRef);

    record ExtractionResult(
            String extractedText,
            int pageCount,
            String extractionMethod,
            int outputChars,
            long inputBytes,
            String sha256,
            boolean wasTruncated
    ) {
    }
}
