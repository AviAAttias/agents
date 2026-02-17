# financial-extraction-worker: extract_financials

## Conductor task
- Task name: `extract_financials`
- Inputs:
  - `jobId`
  - `docType` (from classify_doc output)
  - `text` (from extract_text output)
- Required output:
  - `artifactRef` (format `fin:{id}`)

## Validation and normalization
- Validates `jobId`, `docType`, and `text` are present/non-blank.
- Enforces `FINANCIAL_EXTRACTION_MAX_TEXT_CHARS` via `ai.operations.financial-extraction.max-text-chars` (default `12000`).
- Deterministic truncation is performed with `substring(0, maxTextChars)`.
- Persists:
  - `input_text_sha256` (SHA-256 over bounded text)
  - `input_char_count` (original text length)
  - `was_truncated` flag.

## OpenAI structured extraction
- Uses operation name `financial_extraction`.
- Uses strict schema file: `src/main/resources/schema/financial-extraction-v1.json`.
- Rejects invalid schema output (no regex repair).
- Performs a single bounded retry only for transient safe errors (`TIMEOUT`, `UPSTREAM_5XX`).
- Logs model, duration, input/output chars, and requestId.

## Canonical schema and persistence
- Canonical JSON shape is stable and includes:
  - `documentType`, `currency`, `periodStart`, `periodEnd`, `totalAmount`, `lineItems`, `confidence`, `provenance`.
- `schemaVersion` is persisted (`v1`).
- Table: `financial_artifact`
  - Unique key: `UNIQUE(job_id, task_type)`.

## Schema versioning policy
- The current schema is `v1`.
- Any backward-incompatible schema change must create a new versioned schema file and store the new `schema_version` value.

## Failure modes
- `INVALID_INPUT`: missing/blank required input or unsupported `docType`.
- `INVALID_SCHEMA_OUTPUT`: model output does not pass JSON schema validation.
- `INVALID_OUTPUT`: mapping/serialization failures.
- OpenAI transient failures (`TIMEOUT`, `UPSTREAM_5XX`) are retried once; if retry fails, error is surfaced.
