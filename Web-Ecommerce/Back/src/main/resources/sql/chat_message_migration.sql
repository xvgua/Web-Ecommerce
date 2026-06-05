-- Chat message migration: add extra_data column
-- Uses conditional ALTER to be safe on re-run / Docker fresh deploy

DROP PROCEDURE IF EXISTS migrate_chat_message;
DELIMITER //
CREATE PROCEDURE migrate_chat_message()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'chat_message' AND COLUMN_NAME = 'extra_data') THEN
        ALTER TABLE chat_message ADD COLUMN extra_data TEXT NULL COMMENT 'Extended data (JSON) for product card etc.' AFTER content_type;
    END IF;
END //
DELIMITER ;
CALL migrate_chat_message();
DROP PROCEDURE IF EXISTS migrate_chat_message;
