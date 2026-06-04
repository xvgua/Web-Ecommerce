-- Seckill / Flash Sale tables
-- Run this SQL against the ecommerce database

CREATE TABLE IF NOT EXISTS seckill_activity (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL COMMENT '活动名称',
    start_time  DATETIME NOT NULL COMMENT '开始时间',
    end_time    DATETIME NOT NULL COMMENT '结束时间',
    status      TINYINT DEFAULT 0 COMMENT '0=未开始 1=进行中 2=已结束',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
