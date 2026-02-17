CREATE TABLE IF NOT EXISTS classification_artifact (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    task_type VARCHAR(120) NOT NULL,
    raw_response_json TEXT NOT NULL,
    mapped_result_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_classification_artifact_job_task
    ON classification_artifact(job_id, task_type);
