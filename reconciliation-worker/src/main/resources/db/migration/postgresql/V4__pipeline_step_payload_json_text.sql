ALTER TABLE pipeline_step
  ALTER COLUMN payload_json TYPE TEXT;

ALTER TABLE pipeline_step
  ALTER COLUMN payload_json SET NOT NULL;
