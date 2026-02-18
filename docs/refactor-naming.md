# Repository identity refactor audit: `com.av.agents`

## 1) Inventory and impact analysis

### Identifier search

Repo-wide searches were executed for:
- `com[.]example`
- `com[.]example[.]agents`
- `package com.`
- `groupId`
- `artifactId`
- `spring.application.name`
- `@ComponentScan`
- `scanBasePackages`

Result summary:
- `com[.]example` occurrences: `0`
- `com[.]example[.]agents` occurrences: `0`
- Java package roots discovered: all roots are under `com.av.agents.*`

### Maven modules from root `pom.xml`

- `common-lib`
- `config-server`
- `pdf-ingestion-service`
- `text-extraction-worker`
- `classification-worker`
- `financial-extraction-worker`
- `reconciliation-worker`
- `reporting-worker`
- `approval-service`
- `notification-worker`

### Module coordinates (effective)

All modules inherit parent coordinates from the root POM:

| Module | Effective groupId | artifactId | Effective version |
|---|---|---|---|
| common-lib | com.av.agents | common-lib | 1.0.0-SNAPSHOT |
| config-server | com.av.agents | config-server | 1.0.0-SNAPSHOT |
| pdf-ingestion-service | com.av.agents | pdf-ingestion-service | 1.0.0-SNAPSHOT |
| text-extraction-worker | com.av.agents | text-extraction-worker | 1.0.0-SNAPSHOT |
| classification-worker | com.av.agents | classification-worker | 1.0.0-SNAPSHOT |
| financial-extraction-worker | com.av.agents | financial-extraction-worker | 1.0.0-SNAPSHOT |
| reconciliation-worker | com.av.agents | reconciliation-worker | 1.0.0-SNAPSHOT |
| reporting-worker | com.av.agents | reporting-worker | 1.0.0-SNAPSHOT |
| approval-service | com.av.agents | approval-service | 1.0.0-SNAPSHOT |
| notification-worker | com.av.agents | notification-worker | 1.0.0-SNAPSHOT |

### Spring Boot entrypoints and scanning

| Entrypoint class | Package | scanBasePackages | @ComponentScan | @EntityScan | @EnableJpaRepositories |
|---|---|---|---|---|---|
| `approval/ApprovalServiceApplication` | `com.av.agents.approval` | none | none | none | none |
| `approvalservice/ApprovalServiceApplication` | `com.av.agents.approvalservice` | none | none | none | none |
| `classificationworker/ClassificationWorkerApplication` | `com.av.agents.classificationworker` | none | none | `com.av.agents.classificationworker.entity` | `com.av.agents.classificationworker.repository` |
| `configserver/ConfigServerApplication` | `com.av.agents.configserver` | none | none | none | none |
| `financialextractionworker/FinancialExtractionWorkerApplication` | `com.av.agents.financialextractionworker` | none | none | none | none |
| `notificationworker/NotificationWorkerApplication` | `com.av.agents.notificationworker` | none | none | none | none |
| `pdfingestionservice/PdfIngestionServiceApplication` | `com.av.agents.pdfingestionservice` | none | none | none | none |
| `reconciliationworker/ReconciliationWorkerApplication` | `com.av.agents.reconciliationworker` | none | none | none | none |
| `reportingworker/ReportingWorkerApplication` | `com.av.agents.reportingworker` | none | none | none | none |
| `textextractionworker/TextExtractionWorkerApplication` | `com.av.agents.textextractionworker` | none | none | none | none |

## 2) Maven coordinate migration verification

Root aggregator already uses:
- `<groupId>com.av.agents</groupId>`

All module POMs inherit `com.av.agents` from the parent and do not hardcode `com[.]example`.

`help:evaluate` was attempted for root and all modules using:
- `mvn -B -q -DskipTests help:evaluate -Dexpression=project.groupId -DforceStdout`
- `mvn -B -q -pl <module> -DskipTests help:evaluate -Dexpression=project.groupId -DforceStdout`

In this execution environment, Maven dependency resolution failed before evaluation completed due upstream `repo.maven.apache.org` HTTP 403 responses.

## 3) Java package migration verification

All Java package declarations are under `com.av.agents.*`.
No package declarations were found outside `com.av.agents`.

## 4) Configuration/infrastructure references

Repo-wide search found no `com[.]example` or `com[.]example[.]agents` references in YAML/properties/JSON/README/Helm/Docker/CI files.

## 5) Hard no-leftovers gate

Queries and counts:
- `com[.]example` => `0`
- `com[.]example[.]agents` => `0`
- non-`com.av.agents` Java package roots => `0`

## 6) Build and runtime checks

Executed checks:
- `mvn -B clean verify` → failed in environment due Maven Central HTTP 403 when resolving BOM imports.
- `docker compose up --build -d` → failed in environment (`docker` command unavailable).

