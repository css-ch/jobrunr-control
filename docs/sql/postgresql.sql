-- PostgreSQL DDL for JobRunr Control parameter sets table
-- Create this table manually before using external parameter storage

CREATE TABLE IF NOT EXISTS jobrunr_control_parameter_sets (
    id VARCHAR(36) PRIMARY KEY NOT NULL,
    job_type VARCHAR(500) NOT NULL,
    parameters_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_param_set_job_type ON jobrunr_control_parameter_sets(job_type);
CREATE INDEX IF NOT EXISTS idx_param_set_created ON jobrunr_control_parameter_sets(created_at);
CREATE INDEX IF NOT EXISTS idx_param_set_updated ON jobrunr_control_parameter_sets(updated_at);

COMMENT ON TABLE jobrunr_control_parameter_sets IS 'Stores job parameter sets for JobRunr Control external parameter storage';
COMMENT ON COLUMN jobrunr_control_parameter_sets.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN jobrunr_control_parameter_sets.job_type IS 'Fully qualified job class name';
COMMENT ON COLUMN jobrunr_control_parameter_sets.parameters_json IS 'Job parameters stored as JSON';
COMMENT ON COLUMN jobrunr_control_parameter_sets.created_at IS 'Timestamp when the parameter set was created';
COMMENT ON COLUMN jobrunr_control_parameter_sets.updated_at IS 'Timestamp when the parameter set was last updated';
COMMENT ON COLUMN jobrunr_control_parameter_sets.version IS 'Optimistic locking version';

