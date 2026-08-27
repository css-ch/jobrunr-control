-- Migration from JobRunr Control 2.3.2 to 2.4.0
-- Adds retry-attempt tracking and latest-message filtering to batch messages.
-- Run this script once against an existing 2.3.2 database.

ALTER TABLE `JOBRUNR_CONTROL_BATCH_MESSAGES`
    ADD COLUMN `ATTEMPT_NR` INT NOT NULL DEFAULT 0,
    ADD COLUMN `IS_LATEST` BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_batch_msg_latest
    ON `JOBRUNR_CONTROL_BATCH_MESSAGES`(`BATCH_JOB_ID`, `IS_LATEST`, `CREATED_AT`, `ID`);

CREATE INDEX idx_batch_msg_attempt
    ON `JOBRUNR_CONTROL_BATCH_MESSAGES`(`BATCH_JOB_ID`, `CHILD_JOB_ID`, `IS_LATEST`, `ATTEMPT_NR`);
