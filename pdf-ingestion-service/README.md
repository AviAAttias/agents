# pdf-ingestion-service (`ingest_pdf`)

Conductor worker for ingesting source PDFs and producing an artifact reference for downstream tasks.

## Task contract

### Input
- `jobId` (string)
- `pdfUrl` (string)

### Output
- `artifactRef` (string, required)
- `sha256` (string)
- `bytes` (number)
- `durationMs` (number)

Workflow compatibility:

```text
extract_text.input.artifact = ${ingest_pdf.output.artifactRef}
```

## artifactRef scheme

`artifactRef` uses the deterministic opaque format:

- `pdf:{sha256}`

The file is physically stored in:

- `${PDF_INGESTION_ARTIFACTS_DIR}/${jobId}/${sha256}.pdf`

Default artifacts directory is `artifacts/`.

## Environment variables

- `PDF_INGESTION_MAX_BYTES` (default `26214400`, 25MB)
- `PDF_INGESTION_TIMEOUT_MS` (default `15000`)
- `PDF_INGESTION_ARTIFACTS_DIR` (default `artifacts`)
- `CONDUCTOR_ENABLED` (default `true`)
- `CONDUCTOR_SERVER_URL` (default `http://localhost:8080/api/`)

## Operational limits and behavior

- Fetch timeout is capped via `PDF_INGESTION_TIMEOUT_MS`.
- Downloaded content hard-fails if bytes exceed `PDF_INGESTION_MAX_BYTES`.
- Content-Type is validated when available; obvious non-PDF payloads are rejected.
- A magic-header check (`%PDF-`) is used to reject obvious non-PDF bodies.
- Ingestion computes SHA-256 from payload bytes and stores metadata in `pdf_artifact`.
- Pipeline steps are idempotent by `jobId + taskType`; cached runs return persisted output without refetching URL.
