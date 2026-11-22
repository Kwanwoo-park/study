CREATE TABLE IF NOT EXISTS study_schema_migration (
    version VARCHAR(32) NOT NULL PRIMARY KEY,
    applied_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

SET @study_room_ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'room')
    AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'room' AND column_name = 'direct_room'),
    'ALTER TABLE room ADD COLUMN direct_room BIT(1) NULL',
    'DO 0'
);
PREPARE study_room_stmt FROM @study_room_ddl;
EXECUTE study_room_stmt;
DEALLOCATE PREPARE study_room_stmt;

CREATE TABLE IF NOT EXISTS chat_image_upload (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    room_id BIGINT NULL,
    state VARCHAR(12) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    INDEX idx_chat_image_upload_expiry (expires_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO study_schema_migration (version)
VALUES ('20261123_01')
ON DUPLICATE KEY UPDATE version = study_schema_migration.version;
