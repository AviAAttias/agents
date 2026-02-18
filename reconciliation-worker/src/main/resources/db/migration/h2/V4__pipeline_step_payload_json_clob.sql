ALTER TABLE pipeline_step
  ALTER COLUMN payload_json CLOB;

ALTER TABLE pipeline_step
  ALTER COLUMN payload_json SET NOT NULL;
