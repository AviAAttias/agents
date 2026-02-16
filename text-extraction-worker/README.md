# text-extraction-worker

## Purpose
`text-extraction-worker` is the Conductor worker for task `extract_text` in the financial pipeline. It reads a PDF artifact reference, extracts embedded text with Apache PDFBox, stores the extracted text as an internal text artifact, and returns the Conductor contract outputs consumed by downstream tasks.

## Conductor contract

### Task name
- `extract_text`

### Expected input
```json
{
  "jobId": "job-123",
  "artifact": "file:///data/invoice.pdf"
}
```

### Success output
```json
{
  "text": "...extracted content...",
  "artifactRef": "text-artifact://42",
  "textArtifact": "text-artifact://42",
  "inputBytes": 198123,
  "outputChars": 7542,
  "durationMs": 188,
  "wasTruncated": false,
  "pageCount": 2
}
```

Hard downstream contract keys are:
- `text`
- `artifactRef`

### Failure output
On extraction failure the worker returns `FAILED` with stable payload:
```json
{
  "errorCode": "PDF_MALFORMED",
  "errorMessage": "PDF cannot be parsed"
}
```
`reasonForIncompletion` is set to `<errorCode>: <errorMessage>`.

## How to run locally

From repo root:
```bash
mvn -pl text-extraction-worker -am test
mvn -pl text-extraction-worker spring-boot:run
```

If you want the worker to poll Conductor, set:
```bash
export CONDUCTOR_ENABLED=true
export CONDUCTOR_SERVER_URL=http://localhost:8080/api/
```

## Configuration keys
From `src/main/resources/application.yml`:
- `spring.application.name` (default `text-extraction-worker`)
- `spring.datasource.url` (`DB_URL`, default in-memory H2)
- `spring.datasource.username` (`DB_USERNAME`)
- `spring.datasource.password` (`DB_PASSWORD`)
- `spring.flyway.enabled`
- `management.endpoints.web.exposure.include`
- `conductor.enabled` (`CONDUCTOR_ENABLED`, default `true`)
- `conductor.server.url` (`CONDUCTOR_SERVER_URL`)
- `conductor.worker.thread-count` (`CONDUCTOR_WORKER_THREAD_COUNT`)
- `TEXT_EXTRACTION_MAX_TEXT_CHARS` (default `12000`)

## Limitations
- Extraction mode is **embedded-text only** via PDFBox (`PDFTextStripper`).
- **OCR is not implemented** in this worker.
- For scan/image-only PDFs (no text layer), output `text` is expected to be empty/blank.
- Text is deterministically truncated to `TEXT_EXTRACTION_MAX_TEXT_CHARS` when exceeded.
