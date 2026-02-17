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

CREATE UNIQUE INDEX IF NOT EXISTS uk_classification_pipeline_step_job_task
    ON classification_pipeline_step(job_id, task_type);

CREATE UNIQUE INDEX IF NOT EXISTS uk_classification_pipeline_step_idempotency
    ON classification_pipeline_step(idempotency_key);
