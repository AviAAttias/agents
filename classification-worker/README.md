# classification-worker AI operation

## Conductor task
- Task name: `classify_doc`
- Inputs:
  - `jobId`
  - `textArtifact` (from `${extract_text.output.artifactRef}`)
- Output contract:
  - `documentType` (required)
  - `model`, `durationMs`, `inputChars`, `outputChars`, `schemaName`, `requestId`

## Schema ownership decision
- `classification_pipeline_step` and `classification_artifact` are **owned by classification-worker**.
- `text_artifact` is treated as a **shared cross-module table** produced by text-extraction.
- Therefore this module does not create `text_artifact` in main migrations; tests provide a test-only Flyway migration for deterministic setup.

## Migration strategy
- Flyway is the source of truth for schema.
- Hibernate runs with `ddl-auto=validate` in app and tests.
- Migrations in this module:
  - `V1__create_pipeline_step.sql` => creates `classification_pipeline_step`
  - `V2__create_classification_artifact.sql` => creates `classification_artifact`

## Payload storage strategy
- JSON/text payload columns are mapped as `TEXT` (or compatible LONGVARCHAR semantics), not `@Lob`, to avoid H2/Postgres validation drift.
- Examples:
  - `classification_pipeline_step.payload_json`
  - `classification_artifact.raw_response_json`
  - `classification_artifact.mapped_result_json`

## Artifact resolution
- `textArtifact` must use `text-artifact://<id>`.
- The worker resolves the id from the shared `text_artifact` table and reads `text_body`.
- If the artifact is missing, a typed `ARTIFACT_NOT_FOUND` failure is returned.

## Input constraints
- Max text chars: `CLASSIFICATION_MAX_TEXT_CHARS` (mapped to `classification.worker.max-text-chars`, default `12000`).
- Text is deterministically truncated before the model call when it exceeds the limit.

## OpenAI schema + versioning
- Operation name: `classification`
- Uses `OpenAiJsonClient` (`common-lib`) with strict schema-first JSON validation.
- Current schema name: `classification_document_type_v1`
- Versioning rule: introduce a new schema name suffix (for example `_v2`) when required properties or semantics change.

## Persistence + idempotency
- Idempotency key: `(jobId, taskType)`
- On retry, cached `classification_pipeline_step.payload_json` is returned without re-calling OpenAI.
- Each fresh classification persists one `classification_artifact` row containing:
  - full model response JSON
  - validated mapped result JSON (`documentType`)

## Run tests (module)
```bash
mvn -pl classification-worker test
```
