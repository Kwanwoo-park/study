CREATE TABLE IF NOT EXISTS blocked_ip (
    ip_address VARCHAR(45) NOT NULL,
    blocked_at DATETIME(6) NOT NULL,
    blocked_by_member_id BIGINT NOT NULL,
    PRIMARY KEY (ip_address)
) ENGINE=InnoDB;
