-- MySQL/MariaDB DDL for JobRunr Control parameter sets table
-- Create this table manually before using external parameter storage

CREATE TABLE IF NOT EXISTS jobrunr_control_parameter_sets (
    id VARCHAR(36) PRIMARY KEY NOT NULL,
    job_type VARCHAR(500) NOT NULL,
    parameters_json JSON NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Stores job parameter sets for JobRunr Control external parameter storage';

CREATE INDEX idx_param_set_job_type ON jobrunr_control_parameter_sets(job_type);
CREATE INDEX idx_param_set_created ON jobrunr_control_parameter_sets(created_at);
CREATE INDEX idx_param_set_updated ON jobrunr_control_parameter_sets(updated_at);

