-- Announcement table migration: add status, sort_order, level
ALTER TABLE announcement
    ADD COLUMN status     TINYINT DEFAULT 1 COMMENT '0=draft, 1=published, 2=archived' AFTER content,
    ADD COLUMN sort_order INT     DEFAULT 0 COMMENT 'sort order, higher = first' AFTER status,
    ADD COLUMN level      VARCHAR(20) DEFAULT 'info' COMMENT 'info / warning / important' AFTER sort_order;

-- Update seed data
UPDATE announcement SET status = 1, sort_order = 0, level = 'info' WHERE status IS NULL;
