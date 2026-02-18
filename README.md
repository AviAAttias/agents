# Financial Multi-Agent PDF Pipeline

A multi-module Spring Boot project that processes financial PDFs through a Conductor-orchestrated workflow.

## Modules

| Module | Responsibility | Local run command | Conductor task(s) |
|---|---|---|---|
| `common-lib` | Shared DTOs, enums, OpenAI schema-first client, utility helpers | `mvn -pl common-lib test` | N/A (shared library) |
| `config-server` | Central Spring Cloud Config service | `mvn -pl config-server spring-boot:run` | N/A |
| `pdf-ingestion-service` | Ingests PDF payload metadata/artifact references | `mvn -pl pdf-ingestion-service spring-boot:run` | `ingest_pdf` |
| `text-extraction-worker` | Extracts text from PDFs and stores text artifacts | `mvn -pl text-extraction-worker spring-boot:run` | `extract_text` |
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

Minimal setup:

1. Start supporting services (`docker compose up --build` for local infra/services).
2. Start or point to a running Conductor server.
3. Register task/workflow definitions in Conductor.
4. Export worker/service environment variables (`CONDUCTOR_SERVER_URL`, DB/config vars, and AI vars when needed).

## OpenAI structured-output settings

- `OPENAI_API_KEY` (required for AI workers)
- `OPENAI_BASE_URL` (default `https://api.openai.com`)
- `OPENAI_MODEL` (default `gpt-4o-mini`)
- `OPENAI_MAX_OUTPUT_TOKENS` (default `700`)
- `OPENAI_MAX_INPUT_CHARS` (default `12000`)
- `OPENAI_REQUEST_TIMEOUT_MS` (default `20000`)
- `OPENAI_CONNECT_TIMEOUT_MS` (default `2000`)
- `OPENAI_OVERALL_DEADLINE_MS` (default `22000`)
- `OPENAI_RETRY_ENABLED` (default `true`)
- `OPENAI_RETRY_MAX_ATTEMPTS` (default `1`)

## Run and test from root

```bash
mvn -B clean verify
```

## Notes

- Package and coordinate namespace is standardized on `com.av.agents`.
- Avoid committing secrets; provide credentials via environment variables or secret managers.
