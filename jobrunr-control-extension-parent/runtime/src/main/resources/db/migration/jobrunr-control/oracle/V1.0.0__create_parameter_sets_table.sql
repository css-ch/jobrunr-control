-- Create parameter sets table for external parameter storage
-- Oracle specific migration

CREATE TABLE jobrunr_control_parameter_sets (
    id RAW(16) NOT NULL,
    job_type VARCHAR2(500) NOT NULL,
    parameters_json CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_accessed_at TIMESTAMP NOT NULL,
    version NUMBER(19),
    CONSTRAINT pk_param_sets PRIMARY KEY (id)
);

-- Create indexes for better query performance
CREATE INDEX idx_param_set_job_type ON jobrunr_control_parameter_sets (job_type);
CREATE INDEX idx_param_set_created ON jobrunr_control_parameter_sets (created_at);
CREATE INDEX idx_param_set_accessed ON jobrunr_control_parameter_sets (last_accessed_at);

-- Add comment to table
COMMENT ON TABLE jobrunr_control_parameter_sets IS 'Stores job parameter sets for JobRunr Control external parameter storage';
