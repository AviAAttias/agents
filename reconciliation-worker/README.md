# Reconciliation Worker

## Rule codes
- `TOTAL_MISMATCH`: Sum of `lineItems[*].amount` differs from `totalAmount` beyond tolerance.
- `REQUIRED_FIELD_MISSING`: Required field for `documentType` is missing.
- `CURRENCY_MISMATCH`: Line item currency differs from document currency.
- `INVALID_DATE_RANGE`: `periodStart` is after `periodEnd`.
- `NEGATIVE_TOTAL_NOT_ALLOWED`: Negative totals are rejected unless doc type is explicitly allowed.
- `WARN_UNKNOWN_DOC_TYPE`: Document type has no configured required-field profile.

## Tolerance configuration
- Absolute tolerance is controlled by `reconciliation.tolerance.abs`.
- Default value: `0.01`.
- `TOTAL_MISMATCH` is raised when `abs(totalAmount - sum(lineItems.amount)) > tolerance`.

## Extension mechanism for new rules
1. Add rule evaluation logic in `ReconciliationService#evaluateCanonicalJson`.
2. Emit a structured violation object through `violation(code, path, message)`.
3. Add/update unit tests in `ReconciliationServiceTest` for the new code.
4. If persistence schema changes are required, add a new Flyway migration under `src/main/resources/db/migration`.
