# classification-worker

## Responsibility

Classifies extracted text into domain document classes.

## Owned workflow/task contract

- Conductor task(s): classify_doc
- Input JSON: `{ "jobId": "...", "text": "..." }`
Output JSON: `{ "documentType": "...", "confidence": <number> }`

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
mvn -pl classification-worker spring-boot:run
```

## Tests

```bash
mvn -pl classification-worker test
```

- Validates module unit/integration behavior and task contract serialization where applicable.
