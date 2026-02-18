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
