# text-extraction-worker

## Responsibility

Extracts text from ingested PDF and stores text artifact rows.

## Owned workflow/task contract

- Conductor task(s): extract_text
- Input JSON: `{ "jobId": "...", "artifact": "..." }`
Output JSON: `{ "text": "...", "artifactRef": "text-artifact://<id>", "pageCount": <number> }`

## Env vars and config keys

- `SPRING_PROFILES_ACTIVE`
- `SPRING_CONFIG_IMPORT(optional when using config-server)`
- `CONDUCTOR_SERVER_URL`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Local run

```bash
mvn -pl text-extraction-worker spring-boot:run
```

## Tests

```bash
mvn -pl text-extraction-worker test
```

- Validates module unit/integration behavior and task contract serialization where applicable.
