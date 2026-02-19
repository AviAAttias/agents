CREATE SCHEMA IF NOT EXISTS shared;

CREATE TABLE IF NOT EXISTS shared.approval_request (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    report_artifact_ref VARCHAR(255),
    reviewer_email VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP,
    decision_comment VARCHAR(2000),
    CONSTRAINT uk_approval_request_job_id UNIQUE (job_id)
);
