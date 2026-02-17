ALTER TABLE pipeline_step
    ADD COLUMN IF NOT EXISTS output_json TEXT;

CREATE INDEX IF NOT EXISTS idx_pipeline_step_job_task ON pipeline_step(job_id, task_type);

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
