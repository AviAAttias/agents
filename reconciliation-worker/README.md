# reconciliation-worker

## Responsibility

Validates and reconciles extracted financial values.

## Owned workflow/task contract

- Conductor task(s): validate_reconcile
- Input JSON: `{ "jobId": "...", "fields": { ... } }`
Output JSON: `{ "isValid": <boolean>, "issues": [ ... ] }`

## Env vars and config keys

- `SPRING_PROFILES_ACTIVE`
- `SPRING_CONFIG_IMPORT(optional when using config-server)`
- `CONDUCTOR_SERVER_URL`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Local run

```bash
mvn -pl reconciliation-worker spring-boot:run
```

## Tests

```bash
mvn -pl reconciliation-worker test
```

- Validates module unit/integration behavior and task contract serialization where applicable.
