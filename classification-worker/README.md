# classification-worker AI operation

## Operation
- `classification`
- Uses `OpenAiJsonClient` from `common-lib` with strict JSON schema validation.

## Input constraints
- Request payload must include `text` and it must be non-empty.
- Max text chars: `ai.operations.classification.max-text-chars` (default `12000`).
- If text exceeds limit, it is truncated deterministically and output includes `input_truncated=true`.

## Output schema (overview)
- `label` (string; one of candidate labels)
- `confidence` (number in `[0,1]`)
- `reason` (non-empty string)
- `input_truncated` (boolean)

## Success payload example
```json
{"label":"invoice","confidence":0.98,"reason":"invoice terms matched","input_truncated":false}
```

## Failure payload/code example
- Worker throws typed `PipelineTaskException` with error codes like:
  - `INVALID_INPUT`
  - `INVALID_SCHEMA_OUTPUT`
  - `RATE_LIMIT`
  - `TIMEOUT`
