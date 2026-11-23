ALTER TABLE blocked_ip MODIFY COLUMN blocked_by_member_id BIGINT NULL;

CREATE TABLE IF NOT EXISTS login_ip_attempt (
    ip_address VARCHAR(45) NOT NULL,
    failed_attempts INT NOT NULL,
    PRIMARY KEY (ip_address)
) ENGINE=InnoDB;
