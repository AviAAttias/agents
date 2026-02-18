package com.av.agents.pdfingestionservice.service;

public interface IPdfIngestionService {
    record IngestionPayload(String sha256, long bytes, String artifactRef) {
    }

    IngestionPayload ingest(String jobId, String pdfUrl);
}
