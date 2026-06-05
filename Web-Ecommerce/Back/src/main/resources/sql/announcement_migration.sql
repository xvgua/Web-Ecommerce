-- Announcement table migration: add status, sort_order, level
-- Uses conditional ALTER to be safe on re-run / Docker fresh deploy

DROP PROCEDURE IF EXISTS migrate_announcement;
DELIMITER //
CREATE PROCEDURE migrate_announcement()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'announcement' AND COLUMN_NAME = 'status') THEN
        ALTER TABLE announcement ADD COLUMN status TINYINT DEFAULT 1 COMMENT '0=draft, 1=published, 2=archived' AFTER content;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'announcement' AND COLUMN_NAME = 'sort_order') THEN
        ALTER TABLE announcement ADD COLUMN sort_order INT DEFAULT 0 COMMENT 'sort order, higher = first' AFTER status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'ecommerce' AND TABLE_NAME = 'announcement' AND COLUMN_NAME = 'level') THEN
        ALTER TABLE announcement ADD COLUMN level VARCHAR(20) DEFAULT 'info' COMMENT 'info / warning / important' AFTER sort_order;
    END IF;
END //
DELIMITER ;
CALL migrate_announcement();
DROP PROCEDURE IF EXISTS migrate_announcement;

-- Update seed data
UPDATE announcement SET status = 1, sort_order = 0, level = 'info' WHERE status IS NULL;
