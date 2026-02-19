-- Oracle DDL for JobRunr Control parameter sets table
-- Create this table manually before using external parameter storage
-- This script is idempotent and can be run multiple times safely

DECLARE
    table_exists NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_exists
    FROM user_tables
    WHERE table_name = 'JOBRUNR_CONTROL_PARAMETER_SETS';

    IF table_exists = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE jobrunr_control_parameter_sets (
                id VARCHAR2(36) PRIMARY KEY NOT NULL,
                job_type VARCHAR2(500) NOT NULL,
                parameters_json CLOB NOT NULL,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                version NUMBER(19)
            )
        ';
        EXECUTE IMMEDIATE 'CREATE INDEX idx_param_set_job_type ON jobrunr_control_parameter_sets(job_type)';
        EXECUTE IMMEDIATE 'CREATE INDEX idx_param_set_created ON jobrunr_control_parameter_sets(created_at)';
        EXECUTE IMMEDIATE 'CREATE INDEX idx_param_set_updated ON jobrunr_control_parameter_sets(updated_at)';
        EXECUTE IMMEDIATE 'COMMENT ON TABLE jobrunr_control_parameter_sets IS ''Stores job parameter sets for JobRunr Control external parameter storage''';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN jobrunr_control_parameter_sets.id IS ''Unique identifier (UUID)''';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN jobrunr_control_parameter_sets.job_type IS ''Fully qualified job class name''';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN jobrunr_control_parameter_sets.parameters_json IS ''Job parameters stored as JSON''';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN jobrunr_control_parameter_sets.created_at IS ''Timestamp when the parameter set was created''';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN jobrunr_control_parameter_sets.updated_at IS ''Timestamp when the parameter set was last updated''';
        EXECUTE IMMEDIATE 'COMMENT ON COLUMN jobrunr_control_parameter_sets.version IS ''Optimistic locking version''';
        DBMS_OUTPUT.PUT_LINE('Table jobrunr_control_parameter_sets created successfully.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('Table jobrunr_control_parameter_sets already exists, skipping.');
    END IF;
END;
/
EXIT;
