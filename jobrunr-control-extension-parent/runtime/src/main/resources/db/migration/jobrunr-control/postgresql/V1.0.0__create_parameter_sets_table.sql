-- Create parameter sets table for external parameter storage
-- PostgreSQL specific migration

CREATE TABLE jobrunr_control_parameter_sets
(
    id               UUID         NOT NULL,
    job_type         VARCHAR(500) NOT NULL,
    parameters_json  TEXT         NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    last_accessed_at TIMESTAMP    NOT NULL,
    version          BIGINT,
    CONSTRAINT pk_param_sets PRIMARY KEY (id)
);

-- Create indexes for better query performance
CREATE INDEX idx_param_set_job_type ON jobrunr_control_parameter_sets (job_type);
CREATE INDEX idx_param_set_created ON jobrunr_control_parameter_sets (created_at);
CREATE INDEX idx_param_set_accessed ON jobrunr_control_parameter_sets (last_accessed_at);

-- Add comments for documentation
COMMENT ON TABLE jobrunr_control_parameter_sets IS 'Stores job parameters externally to avoid database limits';
COMMENT ON COLUMN jobrunr_control_parameter_sets.id IS 'Unique identifier for the parameter set';
COMMENT ON COLUMN jobrunr_control_parameter_sets.job_type IS 'Fully qualified class name of the job';
COMMENT ON COLUMN jobrunr_control_parameter_sets.parameters_json IS 'JSON serialized job parameters';
COMMENT ON COLUMN jobrunr_control_parameter_sets.created_at IS 'Timestamp when the parameter set was created';
COMMENT ON COLUMN jobrunr_control_parameter_sets.last_accessed_at IS 'Timestamp of last access for cleanup tracking';
COMMENT ON COLUMN jobrunr_control_parameter_sets.version IS 'Optimistic locking version';
