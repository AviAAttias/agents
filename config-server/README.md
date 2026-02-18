# config-server

## Responsibility

Spring Cloud Config server for centralized configuration.

## Owned workflow/task contract

- Conductor task(s): N/A
- Input: Config client app name/profile requests.
Output: resolved property sources for clients.

## Env vars and config keys

- `SPRING_PROFILES_ACTIVE`
- `SPRING_CONFIG_IMPORT(optional when using config-server)`
- `CONDUCTOR_SERVER_URL`

## Local run

```bash
mvn -pl config-server spring-boot:run
```

## Tests

```bash
mvn -pl config-server test
```

- Validates module unit/integration behavior and task contract serialization where applicable.
