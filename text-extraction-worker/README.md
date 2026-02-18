# text-extraction-worker

## Responsibility

Resolves PDF `artifactRef`, reads bounded bytes with timeout-safe IO, and extracts text using PDFBox.

## Owned workflow/task contract

- Conductor task: `extract_text`
- Input JSON: `{ "jobId": "...", "artifact": "file://...pdf" }`
- Output JSON includes:
  - `text`
  - `artifactRef` / `textArtifact` (text artifact id ref)
  - `inputBytes`, `outputChars`, `pageCount`, `sha256`, `durationMs`, `wasTruncated`

## Artifact resolution

Artifact parsing/resolution is delegated to `common-lib` (`ArtifactRef` + `ArtifactResolver`) instead of worker-local string parsing.

## IO hardening config

- `TEXT_EXTRACTION_MAX_INPUT_BYTES` (default `26214400`)
- `TEXT_EXTRACTION_CONNECT_TIMEOUT_MS` (default `5000`)
- `TEXT_EXTRACTION_READ_TIMEOUT_MS` (default `10000`)
- `TEXT_EXTRACTION_MAX_TEXT_CHARS` (default `12000`)

## Local run

```bash
mvn -pl text-extraction-worker spring-boot:run
```

## Tests

```bash
mvn -pl text-extraction-worker test
```
