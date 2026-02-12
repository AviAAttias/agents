# Financial Multi-Agent PDF Pipeline (Spring Boot + Netflix Conductor Workers)

## Why Maven Multi-Module
Maven multi-module keeps each microservice independently deployable while sharing strict conventions (dependencies, plugin versions, Java version, MapStruct/Lombok settings) from one parent `pom.xml`. This reduces drift, improves CI speed with targeted builds, and keeps Eclipse import straightforward (`Import Existing Maven Projects`).

## Repository Tree

```text
.
├── common-lib
├── config-server
├── pdf-ingestion-service
├── text-extraction-worker
├── classification-worker
├── financial-extraction-worker
├── reconciliation-worker
├── reporting-worker
├── approval-service
├── notification-worker
├── conductor/definitions/{tasks,workflows}
├── helm/financial-pipeline
├── samples/pdfs
├── docker-compose.yml
└── .github/workflows/ci.yml
```

## Implementation Plan
1. Bootstrap parent Maven multi-module project with dependency/plugin management.
2. Add `common-lib` DTO/enums utilities used by services.
3. Build each microservice module with layered architecture (`dto/entity/repository/service/controller/facade`).
4. Add idempotent persistence via Flyway and UNIQUE constraints.
5. Implement approval patch API publishing Conductor event `approval.${jobId}`.
6. Implement exactly-once email worker with idempotency key and MailHog support.
7. Add Conductor task/workflow JSON definitions for external Conductor import.
8. Add observability/security/resilience and OpenAPI exposure.
9. Add Docker, Helm, CI, and sample PDF + demo steps.

## Conductor Definitions
- Tasks: `conductor/definitions/tasks/pipeline_tasks.json`
- Workflow: `conductor/definitions/workflows/financial_pipeline_workflow.json`

Import these JSON files into your already-running Conductor server/UI.

## One-command Demo (excluding Conductor)
```bash
docker compose up --build
```

Then:
1. Start workflow from Conductor using `financial_pipeline` v1.
2. Submit approval edits:
```bash
curl -u pipeline:pipeline -X PATCH http://localhost:<approval-service-port>/api/v1/approvals/<jobId> \
  -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVED","reviewer":"alice","patchedValues":{"total":100.25}}'
```
3. Check sent email in MailHog: `http://localhost:8025`.

## Notes
- Conductor server/UI are intentionally not implemented here.
- Each worker persists `pipeline_step` with unique `(job_id, task_type)` and `idempotency_key` for retry safety.
- H2 is default for local run; Postgres via `DB_URL` env.
