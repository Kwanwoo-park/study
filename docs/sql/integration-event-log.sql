-- Optional manual schema setup when Hibernate ddl-auto=update is not used.
-- No existing table or business data is modified.
CREATE TABLE IF NOT EXISTS integration_event_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    occurred_at DATETIME(6) NOT NULL,
    instance_id VARCHAR(36) NOT NULL,
    broker VARCHAR(10) NOT NULL,
    route VARCHAR(40) NOT NULL,
    operation VARCHAR(30) NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    reference_id BIGINT NULL,
    item_count INT NOT NULL,
    attempt INT NULL,
    subscriber_count BIGINT NULL,
    error_type VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_event_log_time (occurred_at, id),
    KEY idx_event_log_broker (broker, id)
);
