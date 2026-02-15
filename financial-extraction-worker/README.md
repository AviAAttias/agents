# financial-extraction-worker AI operation

## Operation
- `financial_extraction`
- Uses `OpenAiJsonClient` from `common-lib` with strict JSON schema validation.

## Input constraints
- `payloadJson` must be non-empty.
- Max chars: `ai.operations.financial-extraction.max-text-chars` (default `12000`).
- If input exceeds limit, it is truncated deterministically and output includes `input_truncated=true`.

## Output schema (overview)
- `documentType` (string)
- `invoiceNumber` (string)
- `currency` (string)
- `totalAmount` (number)
- `taxAmount` (number)
- `dueDate` (string)
- `confidence` (number `[0,1]`)
- `explanation` (string)
- `input_truncated` (boolean)
- `chunk_count` (number)

## Success payload example
```json
{"documentType":"invoice","currency":"USD","totalAmount":120.0,"confidence":0.95,"explanation":"explicit amount","input_truncated":false,"chunk_count":1}
```

## Failure payload/code example
- Worker throws typed `PipelineTaskException` with error codes:
  - `INVALID_INPUT`
  - `INVALID_OUTPUT`
  - `INVALID_SCHEMA_OUTPUT`
  - `RATE_LIMIT`
  - `TIMEOUT`
