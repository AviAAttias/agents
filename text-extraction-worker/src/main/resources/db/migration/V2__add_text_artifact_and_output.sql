ALTER TABLE pipeline_step
    ADD COLUMN IF NOT EXISTS output_json TEXT;

CREATE INDEX IF NOT EXISTS idx_pipeline_step_job_task ON pipeline_step(job_id, task_type);

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
