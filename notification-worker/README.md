# notification-worker

## Responsibility

Sends notifications (email/events) for workflow milestones.

## Owned workflow/task contract

- Conductor task(s): send_email
- Input JSON: `{ "jobId": "...", "recipient": "...", "subject": "..." }`
Output JSON: `{ "sent": <boolean>, "providerMessageId": "..." }`

## Env vars and config keys

- `SPRING_PROFILES_ACTIVE`
- `SPRING_CONFIG_IMPORT(optional when using config-server)`
- `CONDUCTOR_SERVER_URL`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Local run

```bash
mvn -pl notification-worker spring-boot:run
```

## Tests

```bash
mvn -pl notification-worker test
```

- Validates module unit/integration behavior and task contract serialization where applicable.
