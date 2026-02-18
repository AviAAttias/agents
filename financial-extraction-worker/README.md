# financial-extraction-worker

## Responsibility

Extracts normalized financial fields from classified text.

## Owned workflow/task contract

- Conductor task(s): extract_financials
- Input JSON: `{ "jobId": "...", "text": "...", "documentType": "..." }`
Output JSON: `{ "fields": { ... }, "confidence": <number> }`

## Env vars and config keys

- `SPRING_PROFILES_ACTIVE`
- `SPRING_CONFIG_IMPORT(optional when using config-server)`
- `CONDUCTOR_SERVER_URL`
- `OPENAI_API_KEY`
- `OPENAI_MODEL(optional)`
- `OPENAI_BASE_URL(optional)`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Local run

```bash
mvn -pl financial-extraction-worker spring-boot:run
```

## Tests

```bash
mvn -pl financial-extraction-worker test
```

- Validates module unit/integration behavior and task contract serialization where applicable.
