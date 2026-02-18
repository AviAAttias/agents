# Financial Multi-Agent PDF Pipeline

A multi-module Spring Boot project that processes financial PDFs through a Conductor-orchestrated workflow.

## Artifact reference contract

The pipeline passes artifacts only through explicit `artifactRef` values.

- `file://...` → file-system artifact URI (primary transport for binary artifacts like PDFs)
- `http(s)://...` → remote artifact URI (resolved with connect/read timeouts and byte caps)
- `text-artifact://<id>` → text artifact identifier (DB-backed module contract)
- `fin:<id>` → financial extraction artifact identifier (DB-backed module contract)
- `val:<id>` → reconciliation/validation artifact identifier (DB-backed module contract)
- `report:<id>` → report artifact identifier (DB-backed module contract)

Shared parsing/resolution primitives are in `common-lib`:

- Default global artifact byte cap: `artifacts.max-bytes=10485760` (10 MiB) unless overridden.

- `com.av.agents.common.artifacts.ArtifactRef`
- `com.av.agents.common.artifacts.ArtifactResolver`
- `com.av.agents.common.artifacts.ArtifactResolutionException`

## Modules

| Module | Responsibility | Local run command | Conductor task(s) |
|---|---|---|---|
| `common-lib` | Shared contracts/resolvers/DTOs/helpers | `mvn -pl common-lib test` | N/A |
| `config-server` | Central Spring Cloud Config service | `mvn -pl config-server spring-boot:run` | N/A |
| `pdf-ingestion-service` | Downloads PDF, writes artifact file, emits resolvable PDF `artifactRef` | `mvn -pl pdf-ingestion-service spring-boot:run` | `ingest_pdf` |
| `text-extraction-worker` | Reads PDF artifact ref and extracts text with bounded IO | `mvn -pl text-extraction-worker spring-boot:run` | `extract_text` |
| `classification-worker` | Classifies extracted text into document types | `mvn -pl classification-worker spring-boot:run` | `classify_doc` |
| `financial-extraction-worker` | Extracts structured financial fields from text | `mvn -pl financial-extraction-worker spring-boot:run` | `extract_financials` |
| `reconciliation-worker` | Validates/reconciles extracted values | `mvn -pl reconciliation-worker spring-boot:run` | `validate_reconcile` |
| `reporting-worker` | Produces final reporting artifacts | `mvn -pl reporting-worker spring-boot:run` | `generate_report` |
| `approval-service` | Handles approval requests and reviewer decisions | `mvn -pl approval-service spring-boot:run` | `request_approval` |
| `notification-worker` | Sends notification emails/events | `mvn -pl notification-worker spring-boot:run` | `send_email` |

## Namespace / coordinates

- Maven `groupId`: `com.av.agents`
- Java base package: `com.av.agents`

## Conductor dependency: external vs local

This repository assumes **Conductor is external** (not embedded in module startup).

Required environment/config:

- `CONDUCTOR_SERVER_URL` (example: `http://localhost:8080/api`)
- Task/workflow definitions from:
  - `conductor/definitions/tasks/pipeline_tasks.json`
  - `conductor/definitions/workflows/financial_pipeline_workflow.json`

## Local end-to-end outline

1. Start infra/services (`docker compose up --build`).
2. Start or connect to a running Conductor server.
3. Register Conductor task/workflow definitions.
4. Start all modules (or relevant subset) with shared config.
5. Submit workflow input with `jobId`, `pdfUrl`, requester metadata.

## Run and test

```bash
mvn -B clean verify
```

## Notes

- Avoid committing secrets; provide credentials via environment variables or secret managers.
- See `docs/REPO_EVAL.md` for baseline inventory and currently identified coupling/dependency cleanup follow-ups.
