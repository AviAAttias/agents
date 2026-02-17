# classification-worker AI operation

## Conductor task
- Task name: `classify_doc`
- Inputs:
  - `jobId`
  - `textArtifact` (from `${extract_text.output.artifactRef}`)
- Output contract:
  - `documentType` (required)
  - `model`, `durationMs`, `inputChars`, `outputChars`, `schemaName`, `requestId`

## Artifact resolution
- `textArtifact` must use `text-artifact://<id>`.
- The worker resolves the id from the `text_artifact` table and loads `text_body`.
- If the artifact is missing, a typed `ARTIFACT_NOT_FOUND` failure is returned.

## Input constraints
- Max text chars: `CLASSIFICATION_MAX_TEXT_CHARS` (mapped to `ai.operations.classification.max-text-chars`, default `12000`).
- Text is deterministically truncated before the model call when it exceeds the limit.

## OpenAI schema + versioning
- Operation name: `classification`
- Uses `OpenAiJsonClient` (`common-lib`) with strict schema-first JSON validation.
- Current schema name: `classification_document_type_v1`
- Versioning rule: introduce a new schema name suffix (for example `_v2`) when required properties or semantics change.

## Persistence + idempotency
- Idempotency key: `(jobId, taskType)`
- On retry, cached `pipeline_step.payload_json` is returned without re-calling OpenAI.
- Each fresh classification persists one `classification_artifact` row containing:
  - full model response JSON
  - validated mapped result JSON (`documentType`)

## Failure examples
- `INVALID_INPUT`
- `ARTIFACT_NOT_FOUND`
- `INVALID_SCHEMA_OUTPUT`
- `TIMEOUT`
- `RATE_LIMIT`
