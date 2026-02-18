# reporting-worker

## Responsibility

Generates reporting artifacts for completed pipeline jobs.

## Owned workflow/task contract

- Conductor task(s): generate_report
- Input JSON: `{ "jobId": "...", "reconciliation": { ... } }`
Output JSON: `{ "reportArtifactRef": "...", "summary": "..." }`

## Env vars and config keys

- `SPRING_PROFILES_ACTIVE`
- `SPRING_CONFIG_IMPORT(optional when using config-server)`
- `CONDUCTOR_SERVER_URL`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Local run

```bash
mvn -pl reporting-worker spring-boot:run
```

## Tests

```bash
mvn -pl reporting-worker test
```

- Validates module unit/integration behavior and task contract serialization where applicable.
