# Financial Multi-Agent PDF Pipeline (Spring Boot + Netflix Conductor Workers)

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

## AI Integration

### Modules using OpenAI JSON structured outputs
- `classification-worker` (`classification` operation)
- `financial-extraction-worker` (`financial_extraction` operation)

Both workers use shared `common-lib` `OpenAiJsonClient` with **schema-first strict output validation**.

### Required configuration / env
- `OPENAI_API_KEY` (required)
- `OPENAI_BASE_URL` (optional, default `https://api.openai.com`)
- `OPENAI_MODEL` (default `gpt-4o-mini`)
- `OPENAI_MAX_OUTPUT_TOKENS` (default `700`)
- `OPENAI_MAX_INPUT_CHARS` (default `12000`)
- `OPENAI_REQUEST_TIMEOUT_MS` (default `20000`)
- `OPENAI_CONNECT_TIMEOUT_MS` (default `2000`)
- `OPENAI_OVERALL_DEADLINE_MS` (default `22000`)
- `OPENAI_RETRY_ENABLED` (default `true`)
- `OPENAI_RETRY_MAX_ATTEMPTS` (default `1`)

Worker-specific limits:
- `CLASSIFICATION_MAX_TEXT_CHARS`
- `FINANCIAL_EXTRACTION_MAX_TEXT_CHARS`

### Schema validation behavior
- Every call requires a JSON schema and schema name.
- Model output is parsed as JSON and validated against the schema before mapping.
- Failures are typed and surfaced (e.g. `INVALID_SCHEMA_OUTPUT`) with a bounded `schemaViolationSummary`.
- No best-effort extraction/regex JSON parsing of model output.

### Conductor retry interaction
Conductor already retries tasks. The OpenAI client therefore defaults to **at most one client retry** and only for safe transient classes (`TIMEOUT`, transport/upstream 5xx) to avoid retry amplification.

## Local Run

### 1) Start platform services
```bash
docker compose up --build
```

This starts app services. Use your external/running Conductor instance for workflow/task execution.

### 2) Import Conductor definitions
- `conductor/definitions/tasks/pipeline_tasks.json`
- `conductor/definitions/workflows/financial_pipeline_workflow.json`

### 3) Configure API keys safely
```bash
export OPENAI_API_KEY='***'
export OPENAI_MODEL='gpt-4o-mini'
```
Or use env files / secrets manager. Never commit keys.

## Troubleshooting

### Common OpenAI error codes
- `RATE_LIMIT`: upstream 429, surfaced to Conductor
- `TIMEOUT`: request or overall deadline exceeded
- `INVALID_SCHEMA_OUTPUT`: output not JSON or schema-invalid
- `UPSTREAM_5XX`: upstream 5xx from provider
- `AUTH_CONFIG`: invalid/missing auth or config
- `DISABLED`: OpenAI key absent

### Logging + observability
- Logs include structured fields: `jobId`, `workflowId`, `taskId`, `operation`, `model`, `requestId`, `durationMs`, `inputChars`, `outputChars`.
- Prompt/output bodies are not logged; hash+length are logged.
- Metrics emitted:
  - `openai_requests_total{model,operation,status}`
  - `openai_request_latency_seconds{model,operation}`
  - `openai_schema_validation_failures_total{operation}`
  - `openai_tokens_in_total`
  - `openai_tokens_out_total`
