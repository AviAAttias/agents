# common-lib

## Responsibility

Shared models/utilities used by all modules.

## Owned workflow/task contract

- Conductor task(s): N/A
- Input: library APIs/DTOs used by workers.
Output: validated DTOs, utility results, and OpenAI schema-validated JSON responses.

## Env vars and config keys

- `SPRING_PROFILES_ACTIVE`
- `SPRING_CONFIG_IMPORT(optional when using config-server)`
- `CONDUCTOR_SERVER_URL`

## Local run

```bash
mvn -pl common-lib test
```

## Tests

```bash
mvn -pl common-lib test
```

- Validates module unit/integration behavior and task contract serialization where applicable.
