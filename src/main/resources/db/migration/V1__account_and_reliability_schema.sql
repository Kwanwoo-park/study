CREATE TABLE IF NOT EXISTS account (
    account VARCHAR(255) NOT NULL PRIMARY KEY,
    amount BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(30) NOT NULL DEFAULT 'DEPOSIT_WITHDRAWAL',
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    member_id BIGINT NULL
);

ALTER TABLE account ADD COLUMN IF NOT EXISTS account_type VARCHAR(30) NOT NULL DEFAULT 'DEPOSIT_WITHDRAWAL';
ALTER TABLE account ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE account ADD COLUMN IF NOT EXISTS opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS account_interest (
    account VARCHAR(255) NOT NULL PRIMARY KEY,
    annual_interest_rate DECIMAL(8, 6) NOT NULL,
    maturity_at DATETIME NOT NULL,
    term_months INT NOT NULL,
    last_calculated_at DATETIME NOT NULL,
    accrued_interest DECIMAL(24, 10) NOT NULL DEFAULT 0,
    paid_at DATETIME NULL,
    paid_amount BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_account_interest_account FOREIGN KEY (account) REFERENCES account(account)
);

CREATE TABLE IF NOT EXISTS savings_auto_transfer (
    savings_account VARCHAR(255) NOT NULL PRIMARY KEY,
    source_account VARCHAR(255) NULL,
    monthly_amount BIGINT NOT NULL,
    payment_day INT NOT NULL,
    next_payment_date DATE NULL,
    last_failure_notification_date DATE NULL,
    CONSTRAINT fk_savings_account FOREIGN KEY (savings_account) REFERENCES account(account),
    CONSTRAINT fk_savings_source_account FOREIGN KEY (source_account) REFERENCES account(account)
);

CREATE TABLE IF NOT EXISTS image_cleanup_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    image_url VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL,
    dead_lettered BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS system_incident (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    occurred_at DATETIME NOT NULL,
    occurrence_count BIGINT NOT NULL DEFAULT 1,
    request_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    http_status INT NOT NULL,
    exception_type VARCHAR(255) NOT NULL,
    error_message VARCHAR(1000) NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at DATETIME NULL
);

ALTER TABLE system_incident ADD COLUMN IF NOT EXISTS occurrence_count BIGINT NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS kafka_outbox_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    topic VARCHAR(100) NOT NULL,
    event_key VARCHAR(255) NULL,
    payload_type VARCHAR(30) NOT NULL,
    payload TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL
);
