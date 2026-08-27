-- Migration from JobRunr Control 2.3.2 to 2.4.0
-- Adds retry-attempt tracking and latest-message filtering to batch messages.

DECLARE
    column_exists NUMBER;
    index_exists NUMBER;
BEGIN
    SELECT COUNT(*) INTO column_exists
    FROM user_tab_columns
    WHERE table_name = 'JOBRUNR_CONTROL_BATCH_MESSAGES'
      AND column_name = 'ATTEMPT_NR';

    IF column_exists = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE "JOBRUNR_CONTROL_BATCH_MESSAGES" ADD ("ATTEMPT_NR" NUMBER(10) DEFAULT 0 NOT NULL)';
    END IF;

    SELECT COUNT(*) INTO column_exists
    FROM user_tab_columns
    WHERE table_name = 'JOBRUNR_CONTROL_BATCH_MESSAGES'
      AND column_name = 'IS_LATEST';

    IF column_exists = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE "JOBRUNR_CONTROL_BATCH_MESSAGES" ADD ("IS_LATEST" NUMBER(1) DEFAULT 1 NOT NULL)';
    END IF;

    SELECT COUNT(*) INTO index_exists
    FROM user_indexes
    WHERE index_name = 'IDX_BATCH_MSG_LATEST';

    IF index_exists = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_batch_msg_latest ON "JOBRUNR_CONTROL_BATCH_MESSAGES"("BATCH_JOB_ID", "IS_LATEST", "CREATED_AT", "ID")';
    END IF;

    SELECT COUNT(*) INTO index_exists
    FROM user_indexes
    WHERE index_name = 'IDX_BATCH_MSG_ATTEMPT';

    IF index_exists = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_batch_msg_attempt ON "JOBRUNR_CONTROL_BATCH_MESSAGES"("BATCH_JOB_ID", "CHILD_JOB_ID", "IS_LATEST", "ATTEMPT_NR")';
    END IF;
END;
/
