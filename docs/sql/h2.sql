-- H2 DDL for JobRunr Control parameter sets table
-- Create this table manually before using external parameter storage
--
-- IMPORTANT: H2 must run in STANDARD MODE (not Oracle mode)
-- JobRunr generates H2-specific SQL (e.g., LIMIT syntax) which is incompatible with Oracle mode.
-- If you need Oracle compatibility testing, use actual Oracle database (see oracle.sql).

CREATE TABLE IF NOT EXISTS jobrunr_control_parameter_sets (
    id VARCHAR(36) PRIMARY KEY NOT NULL,
    job_type VARCHAR(500) NOT NULL,
    parameters_json CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);


CREATE INDEX IF NOT EXISTS idx_param_set_job_type ON jobrunr_control_parameter_sets(job_type);
CREATE INDEX IF NOT EXISTS idx_param_set_created ON jobrunr_control_parameter_sets(created_at);
CREATE INDEX IF NOT EXISTS idx_param_set_updated ON jobrunr_control_parameter_sets(updated_at);

