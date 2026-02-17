CREATE TABLE IF NOT EXISTS financial_artifact (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    task_type VARCHAR(120) NOT NULL,
    canonical_json jsonb not null,
    document_type VARCHAR(80) NOT NULL,
    currency VARCHAR(16),
    total_amount NUMERIC(19,4),
    period_start DATE,
    period_end DATE,
    input_text_sha256 VARCHAR(64) NOT NULL,
    input_char_count INTEGER NOT NULL,
    was_truncated BOOLEAN NOT NULL,
    model VARCHAR(120) NOT NULL,
    schema_version VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_financial_artifact_job_task UNIQUE (job_id, task_type)
);
