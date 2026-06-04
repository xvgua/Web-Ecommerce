ALTER TABLE chat_message ADD COLUMN extra_data TEXT NULL COMMENT 'Extended data (JSON) for product card etc.' AFTER content_type;
