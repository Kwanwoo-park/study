CREATE TABLE IF NOT EXISTS email_verification_attempt (
    email_digest VARCHAR(64) NOT NULL,
    previous_request_at DATETIME(6) NULL,
    last_request_at DATETIME(6) NULL,
    blocked_at DATETIME(6) NULL,
    PRIMARY KEY (email_digest)
) ENGINE=InnoDB;
