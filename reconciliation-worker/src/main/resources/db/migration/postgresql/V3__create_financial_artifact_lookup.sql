CREATE TABLE IF NOT EXISTS financial_artifact (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    task_type VARCHAR(120) NOT NULL,
    canonical_json JSON NOT NULL
);
