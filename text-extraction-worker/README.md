# text-extraction-worker

This worker implements Conductor task `extract_text` for PDF extraction.

## Inputs
- `jobId`
- `artifact` (PDF pointer; supports absolute file path, `file://...`, and `http(s)://...`)

## Outputs
- `text`
- `artifactRef`
- `textArtifact` (backward-compatible alias)
- `inputBytes`
- `outputChars`
- `durationMs`
- `wasTruncated`
- `pageCount`

`artifactRef`/`textArtifact` structure: `text-artifact://<text_artifact.id>`.

## Environment variables
- `TEXT_EXTRACTION_MAX_TEXT_CHARS` (default `12000`)
- `CONDUCTOR_ENABLED` (default `true`)
- `CONDUCTOR_SERVER_URL` (default `http://localhost:8080/api/`)
- `CONDUCTOR_WORKER_THREAD_COUNT` (default `2`)
- plus shared DB settings: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

## Operational notes
- Extraction engine: Apache PDFBox.
- CPU/memory profile scales with document size and page count; PDFs are loaded fully in memory for deterministic hashing and extraction, so provision heap accordingly.
- Typical latency for small invoices is low hundreds of milliseconds; larger scanned PDFs can take seconds.
- Hard limit truncates extracted text deterministically at configured max chars and sets `wasTruncated=true`.
- Raw extracted text is not logged.
