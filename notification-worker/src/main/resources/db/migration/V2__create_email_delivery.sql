CREATE TABLE IF NOT EXISTS email_delivery (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(120) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    report_artifact_ref VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    provider_message_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_email_delivery_job_recipient_artifact
    ON email_delivery(job_id, recipient, report_artifact_ref);
