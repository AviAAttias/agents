# Changelog

## Unreleased
- Removed duplicated `com.av.agents.approvalservice` package tree and kept `com.av.agents.approval` as the single approval-service root.
- Added `appr` artifact scheme support in `ArtifactRef` and documented it in the root README.
- Introduced `db-migrations` as the single Flyway migration owner for the shared schema; disabled Flyway by default in services/workers.
- Introduced `shared-persistence-lib` and centralized shared entities/repositories (`TextArtifactEntity`, `FinancialArtifactEntity`, `ValidationArtifactEntity`, `ReportArtifactEntity`, `ApprovalRequestEntity`, `EmailDeliveryEntity`).
- Enforced interface/repository `I*` naming conventions across renamed interfaces and repository types.
- Added `architecture-tests` module with ArchUnit rules for interface/repository/entity conventions and shared-entity ownership.
- Added CI guard step to fail when `target/` build outputs are present.
