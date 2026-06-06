USE ecommerce_product;
-- ============================================
CREATE TABLE IF NOT EXISTS `category` (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name        VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id   BIGINT               DEFAULT 0 COMMENT '父分类ID，0=顶级',
    sort_order  INT                  DEFAULT 0 COMMENT '排序',
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS `product` (
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    name         VARCHAR(200)   NOT NULL COMMENT '商品名称',
    category_id  BIGINT                  DEFAULT NULL COMMENT '分类ID',
    price        DECIMAL(10,2)  NOT NULL COMMENT '价格',
    stock        INT                    DEFAULT 0 COMMENT '库存',
    description  VARCHAR(1000)          DEFAULT NULL COMMENT '简介',
    detail       MEDIUMTEXT             DEFAULT NULL COMMENT '详情(HTML)',
    main_image   VARCHAR(500)           DEFAULT NULL COMMENT '主图URL',
    images       VARCHAR(2000)          DEFAULT NULL COMMENT '图片列表(JSON)',
    status       TINYINT                DEFAULT 1 COMMENT '0=下架 1=上架',
    sales        INT                    DEFAULT 0 COMMENT '销量',
    avg_rating   DECIMAL(3,2)           DEFAULT 0.00 COMMENT '平均评分',
    review_count INT                    DEFAULT 0 COMMENT '评价数',
    listed_at    DATETIME               DEFAULT NULL COMMENT '上架时间',
    create_time  DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_category (category_id),
    KEY idx_status (status),
    KEY idx_sales (sales),
    KEY idx_rating (avg_rating),
    FULLTEXT KEY ft_name (name) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE IF NOT EXISTS `product_sku` (
    id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
    product_id BIGINT         NOT NULL COMMENT '商品ID',
    spec_name  VARCHAR(100)   NOT NULL COMMENT '规格名',
    spec_value VARCHAR(200)            DEFAULT '' COMMENT '规格值',
    price      DECIMAL(10,2)  NOT NULL COMMENT 'SKU价格',
    stock      INT                     DEFAULT 0 COMMENT 'SKU库存',
    sales      INT                     DEFAULT 0 COMMENT 'SKU销量',
    status     TINYINT                 DEFAULT 1 COMMENT '0=禁用 1=启用',
    image      VARCHAR(500)            DEFAULT '' COMMENT 'SKU图片',
    PRIMARY KEY (id),
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

CREATE TABLE IF NOT EXISTS `banner` (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Banner ID',
    title       VARCHAR(100) NOT NULL COMMENT '标题',
    image_url   VARCHAR(500) NOT NULL COMMENT '图片URL',
    link_url    VARCHAR(500)          DEFAULT NULL COMMENT '跳转链接',
    sort_order  INT                   DEFAULT 0 COMMENT '排序',
    status      TINYINT               DEFAULT 1 COMMENT '0=禁用 1=启用',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';

CREATE TABLE IF NOT EXISTS `favorite` (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    product_id  BIGINT NOT NULL COMMENT '商品ID',
    sku_id      BIGINT          DEFAULT NULL COMMENT 'SKU ID',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- Seckill / Flash Sale tables
-- Run this SQL against the ecommerce database

CREATE TABLE IF NOT EXISTS seckill_activity (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100) NOT NULL COMMENT '活动名称',
    background_image VARCHAR(500) DEFAULT NULL COMMENT '活动背景图URL',
    start_time       DATETIME NOT NULL COMMENT '开始时间',
    end_time         DATETIME NOT NULL COMMENT '结束时间',
    status           TINYINT DEFAULT 0 COMMENT '0=未开始 1=进行中 2=已结束',
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS seckill_product (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id     BIGINT NOT NULL COMMENT '秒杀活动ID',
    product_id      BIGINT NOT NULL COMMENT '商品ID',
    sku_id          BIGINT DEFAULT 0 COMMENT 'SKU ID，0=商品级别',
    seckill_price   DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    seckill_stock   INT NOT NULL COMMENT '秒杀总库存',
    remain_stock    INT NOT NULL COMMENT '剩余库存',
    limit_per_user  INT DEFAULT 1 COMMENT '每人限购数量',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity (activity_id),
    INDEX idx_product (product_id)
);
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

-- Add background_image column to seckill_activity (safe on re-run)
DROP PROCEDURE IF EXISTS migrate_seckill_bg;
DELIMITER //
CREATE PROCEDURE migrate_seckill_bg()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'seckill_activity' AND COLUMN_NAME = 'background_image') THEN
        ALTER TABLE seckill_activity ADD COLUMN background_image VARCHAR(500) DEFAULT NULL COMMENT '活动背景图URL' AFTER name;
    END IF;
END //
DELIMITER ;
CALL migrate_seckill_bg();
DROP PROCEDURE IF EXISTS migrate_seckill_bg;
