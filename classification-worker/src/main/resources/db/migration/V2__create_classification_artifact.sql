CREATE TABLE IF NOT EXISTS classification_artifact (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    task_type VARCHAR(120) NOT NULL,
    raw_response_json TEXT NOT NULL,
    mapped_result_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS text_artifact (
    id BIGINT PRIMARY KEY,
    text_body TEXT NOT NULL
);
