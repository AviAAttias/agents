# approval-service

## Responsibility

Creates approval requests and captures reviewer decisions.

## Owned workflow/task contract

- Conductor task(s): request_approval
- Input JSON: `{ "jobId": "...", "decision": "APPROVED|REJECTED", "reviewer": "..." }`
Output JSON: `{ "decision": "...", "reviewer": "...", "decidedAt": "..." }`

## Env vars and config keys

- `SPRING_PROFILES_ACTIVE`
- `SPRING_CONFIG_IMPORT(optional when using config-server)`
- `CONDUCTOR_SERVER_URL`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Local run

```bash
mvn -pl approval-service spring-boot:run
```

## Tests

```bash
mvn -pl approval-service test
```

- Validates module unit/integration behavior and task contract serialization where applicable.
