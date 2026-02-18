# Repository Evaluation Baseline

## Baseline test run

Command run from repository root:

- `mvn -q -DskipTests=false test`

Result:

- Build failed before test execution due to dependency resolution failures from Maven Central (`403 Forbidden`) for Spring Boot and Spring Cloud BOM imports.
- No module tests were executed in this environment because the parent POM could not be fully resolved.

## Pipeline contract mismatches found

1. **Critical break (fixed in this change):**
   - `pdf-ingestion-service` emitted `artifactRef` in format `pdf:<sha256>`.
   - `text-extraction-worker` only supported resolvable URIs/paths (`file://`, `http(s)://`, local paths), so `pdf:` references were unreadable.
2. **Ad-hoc scheme parsing across workers (still present in parts of repo):**
   - Found custom handling for `text-artifact://`, `fin:`, `val:`, and `report:` in worker-specific code.
   - This creates inconsistent interpretation and hidden coupling risk.
3. **Shared-DB coupling risk:**
   - Several workers resolve artifacts using local repositories/tables that are expected to be visible cross-module.
   - This depends on all services sharing one physical database/schema contract.

## Unused starter/dependency inventory candidates

Initial scan identified worker modules with likely extra surface area to prune in follow-up:

- `classification-worker`: includes web/security stack while also operating as a Conductor worker.
- `notification-worker`: includes controller + persistence paths in addition to worker flow.
- `reporting-worker`: includes pipeline-step CRUD style paths and worker logic.
- Additional workers should be reviewed for web/security/JPA/Flyway dependencies that are not required for runtime role.

## What was addressed immediately

- Standardized ingestion output `artifactRef` to resolvable `file://` URI.
- Added shared artifact reference parsing/resolution primitives in `common-lib`.
- Switched text extraction PDF load path to shared resolver with timeouts and max-byte enforcement.
- Added/updated tests to enforce ingestion→extraction artifact contract and prevent reintroduction of `pdf:` mismatch.
