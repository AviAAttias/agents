# pdf-ingestion-service

## Responsibility

Ingests PDF metadata + artifact location and persists pipeline step state.

## Owned workflow/task contract

- Conductor task(s): ingest_pdf
- Input JSON: `{ "jobId": "...", "artifact": "..." }`
Output JSON: `{ "jobId": "...", "artifactRef": "...", "status": "INGESTED" }`

## Env vars and config keys

- `SPRING_PROFILES_ACTIVE`
- `SPRING_CONFIG_IMPORT(optional when using config-server)`
- `CONDUCTOR_SERVER_URL`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Local run

```bash
mvn -pl pdf-ingestion-service spring-boot:run
```

## Tests

```bash
mvn -pl pdf-ingestion-service test
```

- Validates module unit/integration behavior and task contract serialization where applicable.
