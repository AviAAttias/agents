CREATE TABLE IF NOT EXISTS pipeline_step (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    task_type VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    artifact_ref VARCHAR(255),
    payload_json TEXT,
    idempotency_key VARCHAR(200) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_pipeline_step_job_task ON pipeline_step(job_id, task_type);
CREATE UNIQUE INDEX IF NOT EXISTS uk_pipeline_step_idempotency ON pipeline_step(idempotency_key);
ALTER TABLE pipeline_step ADD COLUMN IF NOT EXISTS output_json TEXT;

CREATE TABLE IF NOT EXISTS classification_pipeline_step (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    task_type VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    artifact_ref VARCHAR(255),
    payload_json TEXT NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_classification_pipeline_step_job_task ON classification_pipeline_step(job_id, task_type);
CREATE UNIQUE INDEX IF NOT EXISTS uk_classification_pipeline_step_idempotency ON classification_pipeline_step(idempotency_key);

CREATE TABLE IF NOT EXISTS text_artifact (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    text_body TEXT NOT NULL,
    was_truncated BOOLEAN NOT NULL,
    page_count INTEGER NOT NULL,
    input_bytes BIGINT NOT NULL,
    output_chars INTEGER NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    extraction_method VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_text_artifact_job_id ON text_artifact(job_id);

CREATE TABLE IF NOT EXISTS pdf_artifact (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    source_url VARCHAR(2048) NOT NULL,
    content_length BIGINT NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_pdf_artifact_sha256 ON pdf_artifact(sha256);
CREATE INDEX IF NOT EXISTS idx_pdf_artifact_job_id ON pdf_artifact(job_id);

CREATE TABLE IF NOT EXISTS classification_artifact (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    task_type VARCHAR(120) NOT NULL,
    raw_response_json TEXT NOT NULL,
    mapped_result_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_classification_artifact_job_task ON classification_artifact(job_id, task_type);

CREATE TABLE IF NOT EXISTS financial_artifact (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    task_type VARCHAR(120) NOT NULL,
    canonical_json jsonb NOT NULL,
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

CREATE TABLE IF NOT EXISTS validation_artifact (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    task_type VARCHAR(120) NOT NULL,
    validation_json JSONB NOT NULL,
    validation_status VARCHAR(16) NOT NULL,
    violation_count INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_validation_artifact_job_task ON validation_artifact(job_id, task_type);

CREATE TABLE IF NOT EXISTS report_artifact (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    task_type VARCHAR(120) NOT NULL,
    format VARCHAR(40) NOT NULL,
    content TEXT NOT NULL,
    content_sha256 VARCHAR(64) NOT NULL,
    artifact_ref VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_report_artifact_job_task UNIQUE (job_id, task_type)
);

CREATE TABLE IF NOT EXISTS email_delivery (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    report_artifact_ref VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    provider_message_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_email_delivery_job_recipient_artifact ON email_delivery(job_id, recipient, report_artifact_ref);
