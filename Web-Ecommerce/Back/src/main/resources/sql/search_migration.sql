-- Search module tables
-- Run this SQL against the ecommerce database

CREATE TABLE IF NOT EXISTS search_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword     VARCHAR(200) NOT NULL COMMENT '搜索关键词',
    user_id     BIGINT COMMENT '用户ID（匿名用户为NULL）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    INDEX idx_keyword (keyword),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索日志';

CREATE TABLE IF NOT EXISTS hot_keyword (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword      VARCHAR(200) NOT NULL COMMENT '关键词',
    search_count INT DEFAULT 0 COMMENT '搜索次数',
    is_manual    TINYINT DEFAULT 0 COMMENT '是否手动添加 0=自动 1=手动',
    is_pinned    TINYINT DEFAULT 0 COMMENT '是否置顶 0=否 1=是',
    sort_order   INT DEFAULT 0 COMMENT '排序序号',
    status       TINYINT DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_keyword (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热门搜索词';

-- Add FULLTEXT index on product.name for Chinese (ngram) + English search
-- ngram parser is available in MySQL 5.7.6+/8.0+
ALTER TABLE product ADD FULLTEXT INDEX ft_product_name (name) WITH PARSER ngram;
