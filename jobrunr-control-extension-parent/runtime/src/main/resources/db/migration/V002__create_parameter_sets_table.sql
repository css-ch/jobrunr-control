-- Create table for external parameter storage
CREATE TABLE jobrunr_control_parameter_sets
(
    id               UUID         NOT NULL PRIMARY KEY,
    job_type         VARCHAR(500) NOT NULL,
    parameters_json  TEXT         NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    last_accessed_at TIMESTAMP    NOT NULL,
    version          BIGINT
);

-- Create indexes for performance
CREATE INDEX idx_param_set_job_type ON jobrunr_control_parameter_sets (job_type);
CREATE INDEX idx_param_set_created ON jobrunr_control_parameter_sets (created_at);
CREATE INDEX idx_param_set_accessed ON jobrunr_control_parameter_sets (last_accessed_at);

-- Add comments for documentation
COMMENT ON TABLE jobrunr_control_parameter_sets IS 'Stores job parameters externally when external parameter storage is enabled';
COMMENT ON COLUMN jobrunr_control_parameter_sets.id IS 'Unique identifier for parameter set (referenced by job)';
COMMENT ON COLUMN jobrunr_control_parameter_sets.job_type IS 'Type of job these parameters belong to';
COMMENT ON COLUMN jobrunr_control_parameter_sets.parameters_json IS 'JSON-serialized job parameters';
COMMENT ON COLUMN jobrunr_control_parameter_sets.created_at IS 'Timestamp when parameter set was created';
COMMENT ON COLUMN jobrunr_control_parameter_sets.last_accessed_at IS 'Timestamp when parameter set was last accessed';
