SET @study_blocked_email_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'email_verification_attempt'
              AND column_name = 'blocked_email'),
    'DO 0',
    'ALTER TABLE email_verification_attempt ADD COLUMN blocked_email VARCHAR(254) NULL'
);
PREPARE study_blocked_email_stmt FROM @study_blocked_email_ddl;
EXECUTE study_blocked_email_stmt;
DEALLOCATE PREPARE study_blocked_email_stmt;
