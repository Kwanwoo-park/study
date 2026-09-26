-- Apply only when Hibernate DDL updates are disabled. Existing application tables are untouched.
CREATE TABLE IF NOT EXISTS kafka_dead_letter (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_topic VARCHAR(100) NOT NULL,
    source_partition INT NOT NULL,
    source_offset BIGINT NOT NULL,
    event_key VARCHAR(255),
    event_id VARCHAR(255),
    payload LONGBLOB,
    error_type VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    replayed_at DATETIME(6),
    next_attempt_at DATETIME(6),
    attempt_count INT NOT NULL,
    requested_by BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_kafka_dlt_source UNIQUE (source_topic, source_partition, source_offset),
    INDEX idx_kafka_dlt_replay (status, next_attempt_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
