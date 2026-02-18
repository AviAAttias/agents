# pdf-ingestion-service

## Responsibility

Downloads source PDF, validates size/type, persists artifact metadata, and emits a resolvable PDF artifact reference.

## Owned workflow/task contract

- Conductor task: `ingest_pdf`
- Input JSON: `{ "jobId": "...", "pdfUrl": "https://.../file.pdf" }`
- Output JSON includes:
  - `artifactRef`: `file://.../<jobId>/<sha256>.pdf`
  - `sha256`: SHA-256 digest of downloaded PDF bytes
  - `bytes`, `durationMs`

## IO hardening

- Download connect/request timeout via `pdf.ingestion.timeout-ms`
- Max accepted bytes via `pdf.ingestion.max-bytes`
- Artifact write directory via `pdf.ingestion.artifacts-dir`

## Local run

```bash
mvn -pl pdf-ingestion-service spring-boot:run
```

## Tests

```bash
mvn -pl pdf-ingestion-service test
```
